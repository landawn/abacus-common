package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;

public class IteratorLongStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final long[] values = empty ? new long[0] : new long[] { (long) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final LongStream stream = arrayBacked ? LongStream.of(values)
                        : LongStream.of(com.landawn.abacus.util.LongIterator.of(values))) {
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                            () -> stream.groupTo(value -> value, null, () -> {
                                factoryCalls.incrementAndGet();
                                return new java.util.HashMap<>();
                            }));
                    org.junit.jupiter.api.Assertions.assertEquals(0, factoryCalls.get());
                }
            }
        }
    }

    @Test
    public void testSkipPreservesProgressWhenSupplierFailsBeforeProducingValue() {
        for (final int entryPoint : new int[] { 0, 1, 2, 3, 4 }) {
            final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
            final java.util.concurrent.atomic.AtomicInteger delivered = new java.util.concurrent.atomic.AtomicInteger();
            try (final LongStream stream = LongStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (long) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final LongIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextLong();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((long) 2, iterator.nextLong());
                assertEquals((long) 3, iterator.nextLong());
                assertFalse(iterator.hasNext());
                assertEquals(5, attempts.get());
                assertEquals(4, delivered.get());
            }
        }
    }

    @Test
    public void testLimitAdvancePreservesQuotaAfterSourceFailure() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger delivered = new java.util.concurrent.atomic.AtomicInteger();
        try (final LongStream stream = LongStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (long) delivered.getAndIncrement();
        }).limit(2)) {
            final LongIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((long) 1, iterator.nextLong());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextLong);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        for (final boolean range : new boolean[] { false, true }) {
            final long size = Long.MAX_VALUE;
            final long expected = range ? Long.MAX_VALUE - 1 : 7L;
            try (final LongStream source = range ? LongStream.range(0, Long.MAX_VALUE) : LongStream.repeat(7L, size)) {
                final LongIteratorEx delegate = source.iteratorEx();
                final long[] advanced = { 0 };
                final LongIteratorEx guard = new LongIteratorEx() {
                    @Override
                    boolean supportsFailureAtomicAdvance() {
                        return delegate.supportsFailureAtomicAdvance();
                    }

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public long nextLong() {
                        // Fail immediately if a regression tries to traverse the huge skipped prefix.
                        assertEquals(size - 1, advanced[0]);
                        return delegate.nextLong();
                    }

                    @Override
                    public void advance(final long n) {
                        delegate.advance(n);
                        advanced[0] += n;
                    }
                };

                try (final LongStream result = LongStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                    final LongIteratorEx iterator = result.iteratorEx();
                    assertTrue(iterator.hasNext());
                    assertEquals(expected, iterator.nextLong());
                    assertFalse(iterator.hasNext());
                    assertEquals(size - 1, advanced[0]);
                }
            }
        }
    }


    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final LongStream stream = LongStream.of(LongIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return 7L;
        })).limit(1)) {
            final LongIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextLong);
            assertTrue(iterator.hasNext());
            assertEquals(7L, iterator.nextLong());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextLong);
            assertEquals(2, attempts.get());
        }
    }


    // Creates an iterator-backed LongStream (IteratorLongStream) rather than array-backed
    private LongStream iter(long... values) {
        return LongStream.of(LongIterator.of(values));
    }

    @Test
    public void testToJdkStreamCloseRunsSourceHandlersOnce() {
        final java.util.concurrent.atomic.AtomicInteger closeCount = new java.util.concurrent.atomic.AtomicInteger();
        final LongStream source = iter(1L, 2L, 3L).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testTakeWhile() {
        long[] result = iter(1L, 2L, 5L, 3L).takeWhile(l -> l < 5).toArray();
        assertEquals(2, result.length);
        assertEquals(1L, result[0]);
        assertEquals(2L, result[1]);
    }

    @Test
    public void testFlatmapCollection() {
        long[] r = iter(1L, 2L).flatmap(n -> java.util.Arrays.asList(n, n * 10L)).toArray();
        assertEquals(4, r.length);
        assertEquals(1L, r[0]);
        assertEquals(10L, r[1]);
        assertEquals(2L, r[2]);
        assertEquals(20L, r[3]);

        // empty stream
        assertEquals(0, iter().flatmap(n -> java.util.Arrays.asList((Long) n)).toArray().length);

        // empty / null collection
        assertEquals(0, iter(1L).flatmap(n -> java.util.Collections.<Long> emptyList()).toArray().length);
        assertEquals(0, iter(1L).flatmap(n -> (java.util.Collection<Long>) null).toArray().length);

        // null elements -> 0L
        long[] withNulls = iter(1L).flatmap(n -> java.util.Arrays.asList((Long) null, 9L)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals(0L, withNulls[0]);
        assertEquals(9L, withNulls[1]);
    }

    // groupTo with map supplier
    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Long>> result = iter(1L, 2L, 3L, 4L).groupTo(l -> (l % 2 == 0), Collectors.toList(), java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains(1L));
        assertTrue(result.get(true).contains(2L));
    }

    @Test
    public void testMapToDouble() {
        double[] result = iter(1L, 2L, 3L).mapToDouble(l -> l * 1.5).toArray();
        assertEquals(3, result.length);
        assertEquals(1.5, result[0], 0.001);
    }

    @Test
    public void testFlatMapToInt() {
        List<Integer> result = iter(1L, 2L, 3L).flatMapToInt(l -> IntStream.of((int) l, (int) (l * 10))).toList();
        assertEquals(6, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(30));
    }

    @Test
    public void testFlatMapToFloat() {
        List<Float> result = iter(1L, 2L).flatMapToFloat(l -> FloatStream.of(l, l + 0.5f)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(2.5f));
    }

    @Test
    public void testFlatMapToDouble() {
        List<Double> result = iter(1L, 2L).flatMapToDouble(l -> DoubleStream.of(l, l + 0.5)).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains(1.0));
        assertTrue(result.contains(2.5));
    }

    @Test
    public void testToLongList() {
        com.landawn.abacus.util.LongList result = iter(1L, 2L, 3L).toLongList();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testToSet() {
        java.util.Set<Long> result = iter(1L, 2L, 1L).toSet();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Long> result = iter(1L, 2L, 1L, 3L).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount(1L));
        assertEquals(1, result.getCount(2L));
    }

    @Test
    public void testToMultiset_NoArg() {
        Multiset<Long> result = iter(1L, 2L, 1L).toMultiset();
        assertNotNull(result);
        assertEquals(2, result.getCount(1L));
    }

    @Test
    public void testReduce_WithIdentity() {
        long result = iter(1L, 2L, 3L).reduce(0L, Long::sum);
        assertEquals(6L, result);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalLong result = iter(1L, 2L, 3L).reduce(Long::sum);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce(Long::sum).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalLong result = iter(3L, 1L, 4L, 2L).min();
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalLong result = iter(3L, 1L, 4L, 2L).max();
        assertTrue(result.isPresent());
        assertEquals(4L, result.get());
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalLong result = iter(3L, 1L, 4L, 2L, 5L).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4L, result.get());
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10L, iter(1L, 2L, 3L, 4L).sum());
        assertEquals(0L, iter().sum());
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter(2L, 4L).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        LongSummaryStatistics stats = iter(1L, 2L, 3L, 4L, 5L).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(1L, stats.getMin());
        assertEquals(5L, stats.getMax());
        assertEquals(15L, stats.getSum());
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter(1L, 2L, 3L).anyMatch(l -> l > 2));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter(1L, 2L, 3L).anyMatch(l -> l > 10));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter(1L, 2L, 3L).allMatch(l -> l > 0));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter(1L, 2L, 3L).allMatch(l -> l > 1));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter(1L, 2L, 3L).noneMatch(l -> l > 10));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter(1L, 2L, 3L).noneMatch(l -> l > 2));
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalLong result = iter(1L, 5L, 2L, 3L).findLast(l -> l < 3L);
        assertTrue(result.isPresent());
        assertEquals(2L, result.get());
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter(1L, 2L, 3L).findLast(l -> l > 10L).isPresent());
    }

    @Test
    public void testAsFloatStream() {
        float[] result = iter(1L, 2L, 3L).asFloatStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testAsDoubleStream() {
        double[] result = iter(1L, 2L, 3L).asDoubleStream().toArray();
        assertEquals(3, result.length);
        assertEquals(1.0, result[0], 0.001);
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.LongStream jdk = iter(1L, 2L, 3L).toJdkStream();
        assertNotNull(jdk);
        assertEquals(6L, jdk.sum());
    }

    @Test
    public void testIfEmpty_NotTriggered() {
        boolean[] called = { false };
        iter(1L).ifEmpty(() -> called[0] = true).count();
        assertFalse(called[0]);
    }

    @Test
    public void testIfEmpty_Triggered() {
        boolean[] called = { false };
        iter().ifEmpty(() -> called[0] = true).count();
        assertTrue(called[0]);
    }

    @Test
    public void testIsEmpty_False() {
        assertFalse(iter(1L, 2L).isEmpty());
    }

    @Test
    public void testIsEmpty_True() {
        assertTrue(iter().isEmpty());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        long[] data = { 1L, 2L, 3L, 4L, 5L };
        // sorted ascending: kthLargest(1) = last element (5)
        assertEquals(5L, iter(data).sorted().kthLargest(1).get());
        // kthLargest(N) = first element (1)
        assertEquals(1L, iter(data).sorted().kthLargest(5).get());
        // middle
        assertEquals(3L, iter(data).sorted().kthLargest(3).get());
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get());
    }

    // sum() must wrap on overflow rather than throw, matching documented behavior
    // (see LongStream#sum: "The sum may overflow if the result exceeds Long.MAX_VALUE
    // or is less than Long.MIN_VALUE.") and matching ArrayLongStream#sum behavior.
    @Test
    public void testSum_OverflowWrapsSilently() {
        long expected = Long.MAX_VALUE + 1L; // wraps to Long.MIN_VALUE
        assertEquals(expected, iter(Long.MAX_VALUE, 1L).sum());

        long expected2 = Long.MIN_VALUE - 1L; // wraps to Long.MAX_VALUE
        assertEquals(expected2, iter(Long.MIN_VALUE, -1L).sum());
    }

    @Test
    public void testTop_RejectsNullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> iter(5L, 2L, 8L).top(2, null));
    }

    @Test
    public void testAverageDoesNotOverflowItsRunningSum() {
        assertEquals((double) Long.MAX_VALUE, iter(Long.MAX_VALUE, Long.MAX_VALUE).average().getAsDouble());
        assertEquals(0d, iter(Long.MAX_VALUE, Long.MIN_VALUE, 1L).average().getAsDouble());
    }

    @Test
    public void testHugeSelectionSizesDoNotPreallocateRequestedCapacity() {
        assertFalse(iter(1L).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
        assertArrayEquals(new long[] { 1L }, iter(1L).top(Integer.MAX_VALUE).toArray());
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final LongStream stream = iter(1L, 2L, 3L).onEach(value -> seen[0]++).skip(2);
        final LongIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals(3L, iterator.nextLong());
        assertEquals(3, seen[0]);
        stream.close();
    }

    // TODO: Remaining IteratorLongStream anonymous LongIteratorEx coverage is tied to internal iterator wrappers; exercise through public stream APIs when a stable external behavior gap appears.
}
