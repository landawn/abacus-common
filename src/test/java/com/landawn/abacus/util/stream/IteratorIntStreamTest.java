package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;

public class IteratorIntStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final int[] values = empty ? new int[0] : new int[] { (int) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final IntStream stream = arrayBacked ? IntStream.of(values)
                        : IntStream.of(com.landawn.abacus.util.IntIterator.of(values))) {
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
            try (final IntStream stream = IntStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (int) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final IntIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextInt();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((int) 2, iterator.nextInt());
                assertEquals((int) 3, iterator.nextInt());
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
        try (final IntStream stream = IntStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (int) delivered.getAndIncrement();
        }).limit(2)) {
            final IntIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((int) 1, iterator.nextInt());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextInt);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        for (final boolean range : new boolean[] { false, true }) {
            final long size = range ? (long) Integer.MAX_VALUE - Integer.MIN_VALUE : Long.MAX_VALUE;
            final int expected = range ? Integer.MAX_VALUE - 1 : 7;
            try (final IntStream source = range ? IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE) : IntStream.repeat(7, size)) {
                final IntIteratorEx delegate = source.iteratorEx();
                final long[] advanced = { 0 };
                final IntIteratorEx guard = new IntIteratorEx() {
                    @Override
                    boolean supportsFailureAtomicAdvance() {
                        return delegate.supportsFailureAtomicAdvance();
                    }

                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public int nextInt() {
                        // Fail immediately if a regression tries to traverse the huge skipped prefix.
                        assertEquals(size - 1, advanced[0]);
                        return delegate.nextInt();
                    }

                    @Override
                    public void advance(final long n) {
                        delegate.advance(n);
                        advanced[0] += n;
                    }
                };

                try (final IntStream result = IntStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                    final IntIteratorEx iterator = result.iteratorEx();
                    assertTrue(iterator.hasNext());
                    assertEquals(expected, iterator.nextInt());
                    assertFalse(iterator.hasNext());
                    assertEquals(size - 1, advanced[0]);
                }
            }
        }
    }


    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final IntStream stream = IntStream.of(IntIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return 7;
        })).limit(1)) {
            final IntIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextInt);
            assertTrue(iterator.hasNext());
            assertEquals(7, iterator.nextInt());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextInt);
            assertEquals(2, attempts.get());
        }
    }


    // Creates an iterator-backed IntStream (IteratorIntStream) rather than array-backed
    private IntStream iter(int... values) {
        return IntStream.of(IntIterator.of(values));
    }

    @Test
    public void testToJdkStreamCloseRunsSourceHandlersOnce() {
        final java.util.concurrent.atomic.AtomicInteger closeCount = new java.util.concurrent.atomic.AtomicInteger();
        final IntStream source = iter(1, 2, 3).onClose(closeCount::incrementAndGet);

        source.toJdkStream().close();
        source.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testTakeWhile() {
        int[] result = iter(1, 2, 5, 3).takeWhile(n -> n < 5).toArray();
        assertEquals(2, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
    }

    @Test
    public void testFlatmapCollection() {
        int[] r = iter(1, 2).flatmap(i -> java.util.Arrays.asList(i, i * 10)).toArray();
        assertEquals(4, r.length);
        assertEquals(1, r[0]);
        assertEquals(10, r[1]);
        assertEquals(2, r[2]);
        assertEquals(20, r[3]);

        // empty stream
        assertEquals(0, iter().flatmap(i -> java.util.Arrays.asList((Integer) i)).toArray().length);

        // empty / null collection
        assertEquals(0, iter(1).flatmap(i -> java.util.Collections.<Integer> emptyList()).toArray().length);
        assertEquals(0, iter(1).flatmap(i -> (java.util.Collection<Integer>) null).toArray().length);

        // null elements -> 0
        int[] withNulls = iter(1).flatmap(i -> java.util.Arrays.asList((Integer) null, 9)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals(0, withNulls[0]);
        assertEquals(9, withNulls[1]);
    }

    @Test
    public void testToIntList() {
        com.landawn.abacus.util.IntList result = iter(1, 2, 3).toIntList();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testToSet() {
        java.util.Set<Integer> result = iter(1, 2, 1).toSet();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Integer> result = iter(1, 2, 1, 3).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount(1));
        assertEquals(1, result.getCount(2));
    }

    @Test
    public void testToMultiset_NoArg() {
        Multiset<Integer> result = iter(1, 2, 1).toMultiset();
        assertNotNull(result);
        assertEquals(2, result.getCount(1));
    }

    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Integer>> result = iter(1, 2, 3, 4).groupTo(n -> (n % 2 == 0), Collectors.toList(), java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains(1));
        assertTrue(result.get(true).contains(2));
    }

    @Test
    public void testReduce_WithIdentity() {
        int result = iter(1, 2, 3).reduce(0, (a, b) -> a + b);
        assertEquals(6, result);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalInt result = iter(1, 2, 3).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(6, result.getAsInt());
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalInt result = iter(3, 1, 4, 2).min();
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalInt result = iter(3, 1, 4, 2).max();
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalInt result = iter(3, 1, 4, 2, 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10, iter(1, 2, 3, 4).sum());
        assertEquals(0, iter().sum());
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter(2, 4).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        IntSummaryStatistics stats = iter(1, 2, 3, 4, 5).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(15, stats.getSum());
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter(1, 2, 3).anyMatch(n -> n > 2));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter(1, 2, 3).anyMatch(n -> n > 10));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter(1, 2, 3).allMatch(n -> n > 0));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter(1, 2, 3).allMatch(n -> n > 1));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter(1, 2, 3).noneMatch(n -> n > 10));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter(1, 2, 3).noneMatch(n -> n > 2));
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalInt result = iter(1, 5, 2, 3).findLast(n -> n < 3);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter(1, 2, 3).findLast(n -> n > 10).isPresent());
    }

    @Test
    public void testIfEmpty_NotTriggered() {
        boolean[] called = { false };
        iter(1).ifEmpty(() -> called[0] = true).count();
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
        assertFalse(iter(1, 2).isEmpty());
    }

    @Test
    public void testIsEmpty_True() {
        assertTrue(iter().isEmpty());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        int[] data = { 1, 2, 3, 4, 5 };
        // sorted ascending: kthLargest(1) = last element (5)
        assertEquals(5, iter(data).sorted().kthLargest(1).get());
        // kthLargest(N) = first element (1)
        assertEquals(1, iter(data).sorted().kthLargest(5).get());
        // middle
        assertEquals(3, iter(data).sorted().kthLargest(3).get());
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get());
    }

    @Test
    public void testTop_RejectsNullComparator() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> iter(5, 2, 8).top(2, null));
    }

    @Test
    public void testHugeSelectionSizesDoNotPreallocateRequestedCapacity() {
        assertFalse(iter(1).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
        assertArrayEquals(new int[] { 1 }, iter(1).top(Integer.MAX_VALUE).toArray());
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final IntStream stream = iter(1, 2, 3).onEach(value -> seen[0]++).skip(2);
        final IntIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals(3, iterator.nextInt());
        assertEquals(3, seen[0]);
        stream.close();
    }

    @Test
    public void testTopRetriesAfterSourceFailureDuringInit() {
        final java.util.concurrent.atomic.AtomicInteger pulls = new java.util.concurrent.atomic.AtomicInteger();
        final IntIterator src = new IntIterator() {
            @Override
            public boolean hasNext() {
                return pulls.get() < 4;
            }

            @Override
            public int nextInt() {
                final int n = pulls.getAndIncrement();
                if (n == 1) {
                    throw new IllegalStateException("top drain failed");
                }
                if (n == 0) {
                    return 10;
                }
                return n;
            }
        };
        final IntIteratorEx iter = IntStream.of(src).top(2).iteratorEx();
        assertThrows(IllegalStateException.class, iter::hasNext);
        assertTrue(iter.hasNext());
        final int[] top = iter.toArray();
        assertEquals(2, top.length);
        assertTrue(top[0] == 10 || top[1] == 10);
    }
}
