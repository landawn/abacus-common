package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

public class IteratorFloatStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final float[] values = empty ? new float[0] : new float[] { (float) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final FloatStream stream = arrayBacked ? FloatStream.of(values)
                        : FloatStream.of(com.landawn.abacus.util.FloatIterator.of(values))) {
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
            try (final FloatStream stream = FloatStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (float) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final FloatIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextFloat();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((float) 2, iterator.nextFloat());
                assertEquals((float) 3, iterator.nextFloat());
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
        try (final FloatStream stream = FloatStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (float) delivered.getAndIncrement();
        }).limit(2)) {
            final FloatIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((float) 1, iterator.nextFloat());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextFloat);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        final long size = Long.MAX_VALUE;
        final float expected = 7f;
        try (final FloatStream source = FloatStream.repeat(7f, size)) {
            final FloatIteratorEx delegate = source.iteratorEx();
            final long[] advanced = { 0 };
            final FloatIteratorEx guard = new FloatIteratorEx() {
                @Override
                boolean supportsFailureAtomicAdvance() {
                    return delegate.supportsFailureAtomicAdvance();
                }

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public float nextFloat() {
                    // Fail immediately if a regression tries to traverse the huge skipped prefix.
                    assertEquals(size - 1, advanced[0]);
                    return delegate.nextFloat();
                }

                @Override
                public void advance(final long n) {
                    delegate.advance(n);
                    advanced[0] += n;
                }
            };

            try (final FloatStream result = FloatStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                final FloatIteratorEx iterator = result.iteratorEx();
                assertTrue(iterator.hasNext());
                assertEquals(expected, iterator.nextFloat());
                assertFalse(iterator.hasNext());
                assertEquals(size - 1, advanced[0]);
            }
        }
    }

    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final FloatStream stream = FloatStream.of(FloatIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return 7f;
        })).limit(1)) {
            final FloatIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextFloat);
            assertTrue(iterator.hasNext());
            assertEquals(7f, iterator.nextFloat());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextFloat);
            assertEquals(2, attempts.get());
        }
    }


    // Creates an iterator-backed FloatStream (IteratorFloatStream) rather than array-backed
    private FloatStream iter(float... values) {
        return FloatStream.of(FloatIterator.of(values));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Float> result = iter(1.0f, 2.0f, 1.0f, 3.0f).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount(1.0f));
        assertEquals(1, result.getCount(2.0f));
    }

    @Test
    public void testFlatmapCollection() {
        float[] r = iter(1f, 2f).flatmap(f -> java.util.Arrays.asList(f, f * 10f)).toArray();
        assertEquals(4, r.length);
        assertEquals(1f, r[0], 0.0001f);
        assertEquals(10f, r[1], 0.0001f);
        assertEquals(2f, r[2], 0.0001f);
        assertEquals(20f, r[3], 0.0001f);

        // empty stream
        assertEquals(0, iter().flatmap(f -> java.util.Arrays.asList((Float) f)).toArray().length);

        // empty / null collection
        assertEquals(0, iter(1f).flatmap(f -> java.util.Collections.<Float> emptyList()).toArray().length);
        assertEquals(0, iter(1f).flatmap(f -> (java.util.Collection<Float>) null).toArray().length);

        // null elements -> 0f
        float[] withNulls = iter(1f).flatmap(f -> java.util.Arrays.asList((Float) null, 7.5f)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals(0f, withNulls[0], 0.0001f);
        assertEquals(7.5f, withNulls[1], 0.0001f);
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Float>> result = iter(1.0f, 2.0f, 3.0f, 4.0f).groupTo(f -> (f % 2 == 0), Collectors.toList(), java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains(1.0f));
        assertTrue(result.get(true).contains(2.0f));
    }

    @Test
    public void testReduce_WithIdentity() {
        float result = iter(1.0f, 2.0f, 3.0f).reduce(0.0f, (a, b) -> a + b);
        assertEquals(6.0f, result, 0.001f);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalFloat result = iter(1.0f, 2.0f, 3.0f).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6.0f, result.get(), 0.001f);
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalFloat result = iter(3.0f, 1.0f, 4.0f, 2.0f).min();
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get(), 0.001f);
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalFloat result = iter(3.0f, 1.0f, 4.0f, 2.0f).max();
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get(), 0.001f);
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalFloat result = iter(3.0f, 1.0f, 4.0f, 2.0f, 5.0f).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0f, result.get(), 0.001f);
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        FloatSummaryStatistics stats = iter(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(1.0f, stats.getMin(), 0.001f);
        assertEquals(5.0f, stats.getMax(), 0.001f);
        assertEquals(15.0f, stats.getSum(), 0.001f);
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 2.0f));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter(1.0f, 2.0f, 3.0f).anyMatch(f -> f > 10.0f));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter(1.0f, 2.0f, 3.0f).allMatch(f -> f > 0.0f));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter(1.0f, 2.0f, 3.0f).allMatch(f -> f > 1.0f));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 10.0f));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter(1.0f, 2.0f, 3.0f).noneMatch(f -> f > 2.0f));
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalFloat result = iter(1.0f, 5.0f, 2.0f, 3.0f).findLast(f -> f < 3.0f);
        assertTrue(result.isPresent());
        assertEquals(2.0f, result.get(), 0.001f);
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter(1.0f, 2.0f, 3.0f).findLast(f -> f > 10.0f).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10.0f, iter(1.0f, 2.0f, 3.0f, 4.0f).sum(), 0.001f);
        assertEquals(0.0f, iter().sum(), 0.001f);
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter(2.0f, 4.0f).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        float[] data = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        // sorted ascending: kthLargest(1) = last element (5.0)
        assertEquals(5.0f, iter(data).sorted().kthLargest(1).get(), 0.001f);
        // kthLargest(N) = first element (1.0)
        assertEquals(1.0f, iter(data).sorted().kthLargest(5).get(), 0.001f);
        // middle
        assertEquals(3.0f, iter(data).sorted().kthLargest(3).get(), 0.001f);
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get(), 0.001f);
    }

    // Verifies NaN propagates through min()/max() so iterator-backed and array-backed
    // FloatStream behave consistently (and consistently with FloatSummaryStatistics, which
    // accumulates with Math.min/Math.max).
    @Test
    public void testMinMax_NaNPropagation_IteratorVsArray() {
        float[] data = { 1.0f, Float.NaN, 2.0f };

        OptionalFloat iterMin = iter(data).min();
        OptionalFloat iterMax = iter(data).max();
        assertTrue(iterMin.isPresent());
        assertTrue(iterMax.isPresent());
        assertTrue(Float.isNaN(iterMin.get()), "iterator min should be NaN when stream contains NaN");
        assertTrue(Float.isNaN(iterMax.get()), "iterator max should be NaN when stream contains NaN");

        // Array-backed produces NaN for both — iterator-backed must agree.
        OptionalFloat arrMin = FloatStream.of(data).min();
        OptionalFloat arrMax = FloatStream.of(data).max();
        assertEquals(Float.isNaN(arrMin.get()), Float.isNaN(iterMin.get()));
        assertEquals(Float.isNaN(arrMax.get()), Float.isNaN(iterMax.get()));
    }

    @Test
    public void testTop_RejectsNullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> iter(5f, 2f, 8f).top(2, null));
    }

    @Test
    public void testHugeSelectionSizesDoNotPreallocateRequestedCapacity() {
        assertFalse(iter(1f).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
        assertArrayEquals(new float[] { 1f }, iter(1f).top(Integer.MAX_VALUE).toArray(), 0f);
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final FloatStream stream = iter(1f, 2f, 3f).onEach(value -> seen[0]++).skip(2);
        final FloatIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals(3f, iterator.nextFloat(), 0f);
        assertEquals(3, seen[0]);
        stream.close();
    }
}
