package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;

/** LongStream reduction contracts. */
public class LongStreamReductionTest extends LongStreamTestSupport {

    // acceptIfNotEmpty

    @Test
    public void testAcceptIfNotEmpty() {
        final long[] sum = { 0 };
        LongStream.of(1L, 2L, 3L).acceptIfNotEmpty(list -> {
            sum[0] = list.sum();
        });
        assertEquals(6L, sum[0]);

        sum[0] = 0;
        LongStream.empty().acceptIfNotEmpty(list -> {
            sum[0] = 999L;
        });
        assertEquals(0L, sum[0]);
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicBoolean called = new AtomicBoolean(false);
        LongStream.empty().acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicLong sum = new AtomicLong(0);
        LongStream.of(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());
    }

    @Test
    public void testAllMatch() {
        assertTrue(LongStream.of(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(LongStream.of(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
        assertTrue(LongStream.empty().allMatch(n -> false));
    }

    @Test
    public void testAllMatchWithException() throws Exception {
        assertTrue(createLongStream(2L, 4L, 6L).allMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(LongStream.of(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(LongStream.of(1L, 2L, 3L).anyMatch(n -> n == 5L));
        assertFalse(LongStream.empty().anyMatch(n -> true));
    }

    @Test
    public void testAnyMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 2L));
        assertFalse(createLongStream(1L, 2L, 3L).anyMatch(n -> n == 5L));
    }

    // applyIfNotEmpty

    @Test
    public void testApplyIfNotEmpty() {
        Long result = LongStream.of(1L, 2L, 3L).applyIfNotEmpty(list -> list.sum()).orElse(0L);
        assertNotNull(result);
        assertEquals(6L, result.longValue());

        Long emptyResult = LongStream.empty().applyIfNotEmpty(list -> 999L).orElseNull();
        assertNull(emptyResult);
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Long> result = LongStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Long> result = LongStream.of(1, 2, 3).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6L, result.get().longValue());
    }

    // average

    @Test
    public void testAverage() {
        OptionalDouble result = LongStream.of(1L, 2L, 3L, 4L, 5L).average();
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);

        assertFalse(LongStream.empty().average().isPresent());
    }

    @Test
    public void testAverage_Empty() {
        OptionalDouble avg = LongStream.empty().average();
        assertFalse(avg.isPresent());
    }

    @Test
    public void testAverage_HappyPath() {
        OptionalDouble avg = LongStream.of(1, 2, 3, 4, 5).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    // count

    @Test
    public void testCount() {
        assertEquals(5, LongStream.of(1L, 2L, 3L, 4L, 5L).count());
        assertEquals(0, LongStream.empty().count());
    }

    @Test
    public void testCount_Empty() {
        long result = LongStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testCount_HappyPath() {
        long result = LongStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, result);
    }

    // elementAt

    @Test
    public void testElementAt() {
        assertEquals(20L, LongStream.of(10L, 20L, 30L).elementAt(1).get());
        assertTrue(LongStream.of(1L, 2L).elementAt(5).isEmpty());
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalLong result = LongStream.of(10, 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalLong result = LongStream.of(10, 20, 30, 40).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    @Test
    public void testSummarizeEmpty() {
        LongSummaryStatistics stats = LongStream.empty().summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    // findAny

    @Test
    public void testFindAny() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).findAny();
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 1L && result.get() <= 3L);
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalLong result = createLongStream(10L, 20L, 30L).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalLong result = LongStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_Parallel() {
        // findAny() on a parallel stream takes the parallel branch
        java.util.OptionalLong result = LongStream.range(1L, 10L).parallel().findAny().toJdkOptional();
        assertTrue(result.isPresent());
        assertTrue(result.getAsLong() >= 1L && result.getAsLong() < 10L);
    }

    @Test
    public void testFindAnyWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findAny(n -> n % 2 == 0);
        assertTrue(result.isPresent());
        assertTrue(result.get() == 2L || result.get() == 4L);
    }

    // findFirst

    @Test
    public void testFindFirst() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        assertFalse(LongStream.empty().findFirst().isPresent());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalLong result = createLongStream(10L, 20L, 30L).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10L, result.getAsLong());
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalLong result = LongStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findFirst(n -> n > 2L);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testFindFirstWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findFirst(n -> n > 3);
        assertTrue(result.isPresent());
        assertEquals(4L, result.getAsLong());
    }

    // findLast

    @Test
    public void testFindLast_Empty() {
        OptionalLong result = LongStream.empty().findLast(x -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_HappyPath() {
        OptionalLong result = LongStream.of(1, 2, 3, 4, 5).findLast(x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).findLast(n -> n % 2 == 0);
        assertTrue(result.isPresent());
        assertEquals(4L, result.get());
    }

    @Test
    public void testFindLastWithPredicateException() throws Exception {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L, 5L).findLast(n -> n < 4);
        assertTrue(result.isPresent());
        assertEquals(3L, result.getAsLong());
    }

    // first

    @Test
    public void testFirst() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).first();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testFirst_Empty() {
        OptionalLong result = LongStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_HappyPath2() {
        OptionalLong result = LongStream.of(5, 3, 1).first();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testForEach() {
        List<Long> result = new ArrayList<>();
        LongStream stream = LongStream.of(1L, 2L, 3L);
        stream.forEach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testForeach() {
        List<Long> result = new ArrayList<>();
        createLongStream(1L, 2L, 3L).foreach(result::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), result);
    }

    @Test
    public void testForeach_HappyPath() {
        List<Long> collected = new ArrayList<>();
        LongStream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1L, 2L, 3L), collected);
    }

    @Test
    public void testForEachIndexed() {
        List<String> result = new ArrayList<>();
        LongStream stream = LongStream.of(10L, 20L, 30L);
        stream.forEachIndexed((i, v) -> result.add(i + ":" + v));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    @Test
    public void testForEachIndexedWithException() throws Exception {
        List<String> result = new ArrayList<>();
        createLongStream(10L, 20L, 30L).forEachIndexed((index, value) -> {
            result.add(index + ":" + value);
        });
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), result);
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        OptionalLong result = LongStream.of(5L, 2L, 8L, 1L, 9L, 3L).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8L, result.get());
    }

    @Test
    public void testKthLargest_HappyPath() {
        OptionalLong result = LongStream.of(3, 1, 4, 1, 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testKthLargest_SortedBranch() {
        OptionalLong result = createLongStream(1L, 2L, 3L, 4L).sorted().kthLargest(2);

        assertTrue(result.isPresent());
        assertEquals(3L, result.getAsLong());
    }

    @Test
    public void testKthLargestOutOfBounds() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).kthLargest(5);
        assertFalse(result.isPresent());
    }

    // last

    @Test
    public void testFindLast() {
        OptionalLong result = LongStream.of(1L, 2L, 3L).last();
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testLast_Empty() {
        OptionalLong result = LongStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalLong result = LongStream.of(5, 3, 1).last();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMax() {
        OptionalLong result = LongStream.of(3L, 1L, 4L, 1L, 5L).max();
        assertTrue(result.isPresent());
        assertEquals(5L, result.get());

        assertFalse(LongStream.empty().max().isPresent());
    }

    @Test
    public void testMin() {
        OptionalLong result = LongStream.of(3L, 1L, 4L, 1L, 5L).min();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());

        assertFalse(LongStream.empty().min().isPresent());
    }

    @Test
    public void testNoneMatch() {
        assertTrue(LongStream.of(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(LongStream.of(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
        assertTrue(LongStream.empty().noneMatch(n -> true));
    }

    @Test
    public void testNoneMatchWithException() throws Exception {
        assertTrue(createLongStream(1L, 3L, 5L).noneMatch(n -> n % 2 == 0));
        assertFalse(createLongStream(1L, 2L, 3L).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Long>> result = LongStream.of(1L, 2L, 3L, 4L, 5L).percentiles();
        assertTrue(result.isPresent());
        assertNotNull(result.get());
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1, 2, 3, 4, 5).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1, result.left().getMin());
        assertEquals(5, result.left().getMax());
    }

    // reduce

    @Test
    public void testReduce() {
        long result = LongStream.of(1L, 2L, 3L, 4L).reduce(0L, Long::sum);
        assertEquals(10L, result);
    }

    @Test
    public void testReduceMultiplication() {
        long result = LongStream.of(2L, 3L, 4L).reduce(1L, (a, b) -> a * b);
        assertEquals(24L, result);
    }

    @Test
    public void testReduceWithIdentity() {
        assertEquals(10, LongStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b));
        assertEquals(0, LongStream.empty().reduce(0, (a, b) -> a + b));
    }

    @Test
    public void testReduceWithoutIdentity() {
        OptionalLong result = LongStream.of(1L, 2L, 3L, 4L).reduce(Long::sum);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get());

        assertFalse(LongStream.empty().reduce(Long::sum).isPresent());
    }

    @Test
    public void testSum() {
        long result = LongStream.of(1L, 2L, 3L, 4L, 5L).sum();
        assertEquals(15L, result);

        assertEquals(0L, LongStream.empty().sum());
    }

    @Test
    public void testsummaryStatistics() {
        LongSummaryStatistics stats = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(5L, stats.getMax());
        assertEquals(15L, stats.getSum());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> result = LongStream.of(1L, 2L, 3L, 4L, 5L).summaryStatisticsAndPercentiles();

        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

}
