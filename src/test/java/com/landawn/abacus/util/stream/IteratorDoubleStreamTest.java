package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalDouble;

public class IteratorDoubleStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final double[] values = empty ? new double[0] : new double[] { (double) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final DoubleStream stream = arrayBacked ? DoubleStream.of(values)
                        : DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(values))) {
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
            try (final DoubleStream stream = DoubleStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (double) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final DoubleIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextDouble();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((double) 2, iterator.nextDouble());
                assertEquals((double) 3, iterator.nextDouble());
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
        try (final DoubleStream stream = DoubleStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (double) delivered.getAndIncrement();
        }).limit(2)) {
            final DoubleIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((double) 1, iterator.nextDouble());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextDouble);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        final long size = Long.MAX_VALUE;
        final double expected = 7d;
        try (final DoubleStream source = DoubleStream.repeat(7d, size)) {
            final DoubleIteratorEx delegate = source.iteratorEx();
            final long[] advanced = { 0 };
            final DoubleIteratorEx guard = new DoubleIteratorEx() {
                @Override
                boolean supportsFailureAtomicAdvance() {
                    return delegate.supportsFailureAtomicAdvance();
                }

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public double nextDouble() {
                    // Fail immediately if a regression tries to traverse the huge skipped prefix.
                    assertEquals(size - 1, advanced[0]);
                    return delegate.nextDouble();
                }

                @Override
                public void advance(final long n) {
                    delegate.advance(n);
                    advanced[0] += n;
                }
            };

            try (final DoubleStream result = DoubleStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                final DoubleIteratorEx iterator = result.iteratorEx();
                assertTrue(iterator.hasNext());
                assertEquals(expected, iterator.nextDouble());
                assertFalse(iterator.hasNext());
                assertEquals(size - 1, advanced[0]);
            }
        }
    }

    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final DoubleStream stream = DoubleStream.of(DoubleIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return 7d;
        })).limit(1)) {
            final DoubleIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextDouble);
            assertTrue(iterator.hasNext());
            assertEquals(7d, iterator.nextDouble());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextDouble);
            assertEquals(2, attempts.get());
        }
    }


    // Creates an iterator-backed DoubleStream (IteratorDoubleStream) rather than array-backed
    private DoubleStream iter(double... values) {
        return DoubleStream.of(DoubleIterator.of(values));
    }

    @Test
    public void testToJdkStreamCloseRunsSourceHandlersOnce() {
        final java.util.concurrent.atomic.AtomicInteger closeCount = new java.util.concurrent.atomic.AtomicInteger();
        final DoubleStream source = iter(1d, 2d, 3d).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Double> result = iter(1.0, 2.0, 1.0, 3.0).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount(1.0));
        assertEquals(1, result.getCount(2.0));
    }

    @Test
    public void testFlatmapCollection() {
        double[] r = iter(1.0, 2.0).flatmap(d -> java.util.Arrays.asList(d, d * 10)).toArray();
        assertEquals(4, r.length);
        assertEquals(1.0, r[0], 0.0001);
        assertEquals(10.0, r[1], 0.0001);
        assertEquals(2.0, r[2], 0.0001);
        assertEquals(20.0, r[3], 0.0001);

        // empty stream
        assertEquals(0, iter().flatmap(d -> java.util.Arrays.asList((Double) d)).toArray().length);

        // empty / null collection
        assertEquals(0, iter(1.0).flatmap(d -> java.util.Collections.<Double> emptyList()).toArray().length);
        assertEquals(0, iter(1.0).flatmap(d -> (java.util.Collection<Double>) null).toArray().length);

        // null elements -> 0d
        double[] withNulls = iter(1.0).flatmap(d -> java.util.Arrays.asList((Double) null, 7.5)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals(0.0, withNulls[0], 0.0001);
        assertEquals(7.5, withNulls[1], 0.0001);
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Double>> result = iter(1.0, 2.0, 3.0, 4.0).groupTo(d -> (d % 2 == 0), Collectors.toList(), java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains(1.0));
        assertTrue(result.get(true).contains(2.0));
    }

    @Test
    public void testReduce_WithIdentity() {
        double result = iter(1.0, 2.0, 3.0).reduce(0.0, (a, b) -> a + b);
        assertEquals(6.0, result, 0.001);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalDouble result = iter(1.0, 2.0, 3.0).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalDouble result = iter(3.0, 1.0, 4.0, 2.0).min();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalDouble result = iter(3.0, 1.0, 4.0, 2.0).max();
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalDouble result = iter(3.0, 1.0, 4.0, 2.0, 5.0).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        DoubleSummaryStatistics stats = iter(1.0, 2.0, 3.0, 4.0, 5.0).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(1.0, stats.getMin(), 0.001);
        assertEquals(5.0, stats.getMax(), 0.001);
        assertEquals(15.0, stats.getSum(), 0.001);
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter(1.0, 2.0, 3.0).anyMatch(d -> d > 2.0));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter(1.0, 2.0, 3.0).anyMatch(d -> d > 10.0));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter(1.0, 2.0, 3.0).allMatch(d -> d > 0.0));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter(1.0, 2.0, 3.0).allMatch(d -> d > 1.0));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter(1.0, 2.0, 3.0).noneMatch(d -> d > 10.0));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter(1.0, 2.0, 3.0).noneMatch(d -> d > 2.0));
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalDouble result = iter(1.0, 5.0, 2.0, 3.0).findLast(d -> d < 3.0);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter(1.0, 2.0, 3.0).findLast(d -> d > 10.0).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10.0, iter(1.0, 2.0, 3.0, 4.0).sum(), 0.001);
        assertEquals(0.0, iter().sum(), 0.001);
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter(2.0, 4.0).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        double[] data = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        // sorted ascending: kthLargest(1) = last element (5.0)
        assertEquals(5.0, iter(data).sorted().kthLargest(1).get(), 0.001);
        // kthLargest(N) = first element (1.0)
        assertEquals(1.0, iter(data).sorted().kthLargest(5).get(), 0.001);
        // middle
        assertEquals(3.0, iter(data).sorted().kthLargest(3).get(), 0.001);
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get(), 0.001);
    }

    // Verifies NaN propagates through min()/max() so iterator-backed and array-backed
    // DoubleStream behave consistently (and consistently with java.util.stream.DoubleStream
    // and DoubleSummaryStatistics, which use Math.min/Math.max).
    @Test
    public void testMinMax_NaNPropagation_IteratorVsArray() {
        double[] data = { 1.0, Double.NaN, 2.0 };

        OptionalDouble iterMin = iter(data).min();
        OptionalDouble iterMax = iter(data).max();
        assertTrue(iterMin.isPresent());
        assertTrue(iterMax.isPresent());
        assertTrue(Double.isNaN(iterMin.getAsDouble()), "iterator min should be NaN when stream contains NaN");
        assertTrue(Double.isNaN(iterMax.getAsDouble()), "iterator max should be NaN when stream contains NaN");

        // Array-backed produces NaN for both — iterator-backed must agree.
        OptionalDouble arrMin = DoubleStream.of(data).min();
        OptionalDouble arrMax = DoubleStream.of(data).max();
        assertEquals(Double.isNaN(arrMin.getAsDouble()), Double.isNaN(iterMin.getAsDouble()));
        assertEquals(Double.isNaN(arrMax.getAsDouble()), Double.isNaN(iterMax.getAsDouble()));
    }

    @Test
    public void testTop_RejectsNullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> iter(5.0, 2.0, 8.0).top(2, null));
    }

    @Test
    public void testHugeSelectionSizesDoNotPreallocateRequestedCapacity() {
        assertFalse(iter(1d).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
        assertArrayEquals(new double[] { 1d }, iter(1d).top(Integer.MAX_VALUE).toArray(), 0d);
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final DoubleStream stream = iter(1d, 2d, 3d).onEach(value -> seen[0]++).skip(2);
        final DoubleIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals(3d, iterator.nextDouble(), 0d);
        assertEquals(3, seen[0]);
        stream.close();
    }
}
