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
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;

public class IteratorShortStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final short[] values = empty ? new short[0] : new short[] { (short) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final ShortStream stream = arrayBacked ? ShortStream.of(values) : ShortStream.of(com.landawn.abacus.util.ShortIterator.of(values))) {
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> stream.groupTo(value -> value, null, () -> {
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
            try (final ShortStream stream = ShortStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (short) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final ShortIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextShort();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((short) 2, iterator.nextShort());
                assertEquals((short) 3, iterator.nextShort());
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
        try (final ShortStream stream = ShortStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (short) delivered.getAndIncrement();
        }).limit(2)) {
            final ShortIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((short) 1, iterator.nextShort());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextShort);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        final long size = Long.MAX_VALUE;
        final short expected = (short) 7;
        try (final ShortStream source = ShortStream.repeat((short) 7, size)) {
            final ShortIteratorEx delegate = source.iteratorEx();
            final long[] advanced = { 0 };
            final ShortIteratorEx guard = new ShortIteratorEx() {
                @Override
                boolean supportsFailureAtomicAdvance() {
                    return delegate.supportsFailureAtomicAdvance();
                }

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public short nextShort() {
                    // Fail immediately if a regression tries to traverse the huge skipped prefix.
                    assertEquals(size - 1, advanced[0]);
                    return delegate.nextShort();
                }

                @Override
                public void advance(final long n) {
                    delegate.advance(n);
                    advanced[0] += n;
                }
            };

            try (final ShortStream result = ShortStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                final ShortIteratorEx iterator = result.iteratorEx();
                assertTrue(iterator.hasNext());
                assertEquals(expected, iterator.nextShort());
                assertFalse(iterator.hasNext());
                assertEquals(size - 1, advanced[0]);
            }
        }
    }

    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final ShortStream stream = ShortStream.of(ShortIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return (short) 7;
        })).limit(1)) {
            final ShortIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextShort);
            assertTrue(iterator.hasNext());
            assertEquals((short) 7, iterator.nextShort());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextShort);
            assertEquals(2, attempts.get());
        }
    }

    // Creates an iterator-backed ShortStream (IteratorShortStream) rather than array-backed
    private ShortStream iter(short... values) {
        return ShortStream.of(ShortIterator.of(values));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Short> result = iter((short) 1, (short) 2, (short) 1, (short) 3).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount((short) 1));
        assertEquals(1, result.getCount((short) 2));
    }

    @Test
    public void testFlatmapCollection() {
        short[] r = iter((short) 1, (short) 2).flatmap(s -> java.util.Arrays.asList((Short) s, (short) (s * 10))).toArray();
        assertEquals(4, r.length);
        assertEquals((short) 1, r[0]);
        assertEquals((short) 10, r[1]);
        assertEquals((short) 2, r[2]);
        assertEquals((short) 20, r[3]);

        // empty stream
        assertEquals(0, iter().flatmap(s -> java.util.Arrays.asList((Short) s)).toArray().length);

        // empty / null collection
        assertEquals(0, iter((short) 1).flatmap(s -> java.util.Collections.<Short> emptyList()).toArray().length);
        assertEquals(0, iter((short) 1).flatmap(s -> (java.util.Collection<Short>) null).toArray().length);

        // null elements -> (short) 0
        short[] withNulls = iter((short) 1).flatmap(s -> java.util.Arrays.asList((Short) null, (short) 9)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals((short) 0, withNulls[0]);
        assertEquals((short) 9, withNulls[1]);
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Short>> result = iter((short) 1, (short) 2, (short) 3, (short) 4).groupTo(s -> (s % 2 == 0), Collectors.toList(),
                java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains((short) 1));
        assertTrue(result.get(true).contains((short) 2));
    }

    @Test
    public void testReduce_WithIdentity() {
        short result = iter((short) 1, (short) 2, (short) 3).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals((short) 6, result);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalShort result = iter((short) 1, (short) 2, (short) 3).reduce((a, b) -> (short) (a + b));
        assertTrue(result.isPresent());
        assertEquals((short) 6, result.get());
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce((a, b) -> (short) (a + b)).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalShort result = iter((short) 3, (short) 1, (short) 4, (short) 2).min();
        assertTrue(result.isPresent());
        assertEquals((short) 1, result.get());
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalShort result = iter((short) 3, (short) 1, (short) 4, (short) 2).max();
        assertTrue(result.isPresent());
        assertEquals((short) 4, result.get());
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalShort result = iter((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals((short) 4, result.get());
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10, iter((short) 1, (short) 2, (short) 3, (short) 4).sum());
        assertEquals(0, iter().sum());
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter((short) 2, (short) 4).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        ShortSummaryStatistics stats = iter((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals((short) 1, stats.getMin());
        assertEquals((short) 5, stats.getMax());
        assertEquals(15, stats.getSum());
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter((short) 1, (short) 2, (short) 3).anyMatch(s -> s > 2));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter((short) 1, (short) 2, (short) 3).anyMatch(s -> s > 10));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter((short) 1, (short) 2, (short) 3).allMatch(s -> s > 0));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter((short) 1, (short) 2, (short) 3).allMatch(s -> s > 1));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 10));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 2));
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalShort result = iter((short) 1, (short) 5, (short) 2, (short) 3).findLast(s -> s < 3);
        assertTrue(result.isPresent());
        assertEquals((short) 2, result.get());
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter((short) 1, (short) 2, (short) 3).findLast(s -> s > 10).isPresent());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        short[] data = { 1, 2, 3, 4, 5 };
        // sorted ascending: kthLargest(1) = last element (5)
        assertEquals((short) 5, iter(data).sorted().kthLargest(1).get());
        // kthLargest(N) = first element (1)
        assertEquals((short) 1, iter(data).sorted().kthLargest(5).get());
        // middle
        assertEquals((short) 3, iter(data).sorted().kthLargest(3).get());
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get());
    }

    @Test
    public void testTop_RejectsNullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> iter((short) 5, (short) 2, (short) 8).top(2, null));
    }

    @Test
    public void testHugeSelectionSizesDoNotPreallocateRequestedCapacity() {
        assertFalse(iter((short) 1).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
        assertArrayEquals(new short[] { 1 }, iter((short) 1).top(Integer.MAX_VALUE).toArray());
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final ShortStream stream = iter((short) 1, (short) 2, (short) 3).onEach(value -> seen[0]++).skip(2);
        final ShortIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals((short) 3, iterator.nextShort());
        assertEquals(3, seen[0]);
        stream.close();
    }
}
