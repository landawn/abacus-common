package com.landawn.abacus.util.stream;

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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;

public class IteratorByteStreamTest extends TestBase {
    @Test
    public void testGroupToRejectsNullDownstreamBeforeInvokingMapFactory() {
        for (final boolean arrayBacked : new boolean[] { true, false }) {
            for (final boolean empty : new boolean[] { true, false }) {
                final byte[] values = empty ? new byte[0] : new byte[] { (byte) 1 };
                final java.util.concurrent.atomic.AtomicInteger factoryCalls = new java.util.concurrent.atomic.AtomicInteger();
                try (final ByteStream stream = arrayBacked ? ByteStream.of(values) : ByteStream.of(com.landawn.abacus.util.ByteIterator.of(values))) {
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
            try (final ByteStream stream = ByteStream.generate(() -> {
                if (attempts.getAndIncrement() == 1) {
                    throw new IllegalStateException("second attempt");
                }
                return (byte) delivered.getAndIncrement();
            }).limit(4).skip(2)) {
                final ByteIteratorEx iterator = stream.iteratorEx();
                assertThrows(IllegalStateException.class, () -> {
                    switch (entryPoint) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.nextByte();
                        case 2 -> iterator.count();
                        case 3 -> iterator.advance(1);
                        case 4 -> iterator.toArray();
                        default -> throw new AssertionError();
                    }
                });
                assertTrue(iterator.hasNext());
                assertEquals((byte) 2, iterator.nextByte());
                assertEquals((byte) 3, iterator.nextByte());
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
        try (final ByteStream stream = ByteStream.generate(() -> {
            if (attempts.getAndIncrement() == 1) {
                throw new IllegalStateException("second attempt");
            }
            return (byte) delivered.getAndIncrement();
        }).limit(2)) {
            final ByteIteratorEx iterator = stream.iteratorEx();
            assertThrows(IllegalStateException.class, () -> iterator.advance(2));
            assertTrue(iterator.hasNext());
            assertEquals((byte) 1, iterator.nextByte());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextByte);
            assertEquals(3, attempts.get());
            assertEquals(2, delivered.get());
        }
    }

    @Test
    public void testSkipPreservesFastBulkAdvanceForHugeSources() {
        final long size = Long.MAX_VALUE;
        final byte expected = (byte) 7;
        try (final ByteStream source = ByteStream.repeat((byte) 7, size)) {
            final ByteIteratorEx delegate = source.iteratorEx();
            final long[] advanced = { 0 };
            final ByteIteratorEx guard = new ByteIteratorEx() {
                @Override
                boolean supportsFailureAtomicAdvance() {
                    return delegate.supportsFailureAtomicAdvance();
                }

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public byte nextByte() {
                    // Fail immediately if a regression tries to traverse the huge skipped prefix.
                    assertEquals(size - 1, advanced[0]);
                    return delegate.nextByte();
                }

                @Override
                public void advance(final long n) {
                    delegate.advance(n);
                    advanced[0] += n;
                }
            };

            try (final ByteStream result = ByteStream.of(guard).limit(size).skip(1).skip(size - 2)) {
                final ByteIteratorEx iterator = result.iteratorEx();
                assertTrue(iterator.hasNext());
                assertEquals(expected, iterator.nextByte());
                assertFalse(iterator.hasNext());
                assertEquals(size - 1, advanced[0]);
            }
        }
    }

    @Test
    public void testLimitPreservesQuotaWhenSupplierFailsBeforeProducingValue() {
        final java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        try (final ByteStream stream = ByteStream.of(ByteIterator.generate(() -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first attempt");
            }
            return (byte) 7;
        })).limit(1)) {
            final ByteIterator iterator = stream.iterator();
            assertTrue(iterator.hasNext());
            assertThrows(IllegalStateException.class, iterator::nextByte);
            assertTrue(iterator.hasNext());
            assertEquals((byte) 7, iterator.nextByte());
            assertFalse(iterator.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iterator::nextByte);
            assertEquals(2, attempts.get());
        }
    }

    // Creates an iterator-backed ByteStream (IteratorByteStream) rather than array-backed
    private ByteStream iter(byte... values) {
        return ByteStream.of(ByteIterator.of(values));
    }

    @Test
    public void testTakeWhile() {
        byte[] result = iter((byte) 1, (byte) 2, (byte) 5, (byte) 3).takeWhile(b -> b < 5).toArray();
        assertEquals(2, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 2, result[1]);
    }

    @Test
    public void testMapToInt() {
        int[] result = iter((byte) 1, (byte) 2, (byte) 3).mapToInt(b -> b * 2).toArray();
        assertEquals(3, result.length);
        assertEquals(2, result[0]);
        assertEquals(4, result[1]);
        assertEquals(6, result[2]);
    }

    @Test
    public void testMapToObj() {
        java.util.List<String> result = iter((byte) 65, (byte) 66).mapToObj(b -> String.valueOf((char) b)).toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("A"));
        assertTrue(result.contains("B"));
    }

    @Test
    public void testFlatMap_ByteStream() {
        byte[] result = iter((byte) 1, (byte) 2).flatMap(b -> ByteStream.of(b, (byte) (b * 2))).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testFlatmap_ByteArray() {
        byte[] result = iter((byte) 1, (byte) 2).flatMapArray(b -> new byte[] { b, (byte) (b + 10) }).toArray();
        assertEquals(4, result.length);
    }

    @Test
    public void testFlatmap_Collection() {
        // multi-element
        byte[] r = iter((byte) 1, (byte) 2).flatmap(b -> java.util.Arrays.asList((Byte) b, (byte) (b * 10))).toArray();
        assertEquals(4, r.length);
        assertEquals(1, r[0]);
        assertEquals(10, r[1]);
        assertEquals(2, r[2]);
        assertEquals(20, r[3]);

        // empty stream
        assertEquals(0, iter().flatmap(b -> java.util.Arrays.asList((Byte) b)).toArray().length);

        // empty collection
        assertEquals(0, iter((byte) 1, (byte) 2).flatmap(b -> java.util.Collections.<Byte> emptyList()).toArray().length);

        // null collection
        assertEquals(0, iter((byte) 1).flatmap(b -> (java.util.Collection<Byte>) null).toArray().length);

        // null elements -> (byte) 0
        byte[] withNulls = iter((byte) 1).flatmap(b -> java.util.Arrays.asList((Byte) null, (byte) 9)).toArray();
        assertEquals(2, withNulls.length);
        assertEquals(0, withNulls[0]);
        assertEquals(9, withNulls[1]);
    }

    @Test
    public void testFlatMapToInt() {
        int[] result = iter((byte) 1, (byte) 2, (byte) 3).flatMapToInt(b -> IntStream.of(b, b * 2)).toArray();
        assertEquals(6, result.length);
    }

    @Test
    public void testFlatmapToObj() {
        java.util.List<Integer> result = iter((byte) 1, (byte) 2).flatmapToObj(b -> java.util.Arrays.asList((int) b, b * 10)).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testToByteList() {
        com.landawn.abacus.util.ByteList result = iter((byte) 1, (byte) 2, (byte) 3).toByteList();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testToSet() {
        java.util.Set<Byte> result = iter((byte) 1, (byte) 2, (byte) 1).toSet();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Byte> result = iter((byte) 1, (byte) 2, (byte) 1, (byte) 3).toMultiset(Multiset::new);
        assertNotNull(result);
        assertEquals(2, result.getCount((byte) 1));
        assertEquals(1, result.getCount((byte) 2));
    }

    @Test
    public void testToMultiset_NoArg() {
        Multiset<Byte> result = iter((byte) 1, (byte) 2, (byte) 1).toMultiset();
        assertNotNull(result);
        assertEquals(2, result.getCount((byte) 1));
    }

    // groupTo(keyMapper, collector, mapSupplier) - uses 3-arg variant
    @Test
    public void testGroupTo_WithMapSupplier() {
        Map<Boolean, List<Byte>> result = iter((byte) 1, (byte) 2, (byte) 3, (byte) 4).groupTo(b -> (b % 2 == 0), Collectors.toList(), java.util.HashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(false).contains((byte) 1));
        assertTrue(result.get(true).contains((byte) 2));
    }

    @Test
    public void testReduce_WithIdentity() {
        byte result = iter((byte) 1, (byte) 2, (byte) 3).reduce((byte) 0, (a, b) -> (byte) (a + b));
        assertEquals((byte) 6, result);
    }

    @Test
    public void testReduce_WithoutIdentity_Present() {
        OptionalByte result = iter((byte) 1, (byte) 2, (byte) 3).reduce((a, b) -> (byte) (a + b));
        assertTrue(result.isPresent());
        assertEquals((byte) 6, result.get());
    }

    @Test
    public void testReduce_WithoutIdentity_Empty() {
        assertFalse(iter().reduce((a, b) -> (byte) (a + b)).isPresent());
    }

    @Test
    public void testMin_Present() {
        OptionalByte result = iter((byte) 3, (byte) 1, (byte) 4, (byte) 2).min();
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testMin_Empty() {
        assertFalse(iter().min().isPresent());
    }

    @Test
    public void testMax_Present() {
        OptionalByte result = iter((byte) 3, (byte) 1, (byte) 4, (byte) 2).max();
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());
    }

    @Test
    public void testMax_Empty() {
        assertFalse(iter().max().isPresent());
    }

    @Test
    public void testKthLargest() {
        OptionalByte result = iter((byte) 3, (byte) 1, (byte) 4, (byte) 2, (byte) 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals((byte) 4, result.get());
    }

    @Test
    public void testKthLargest_Empty() {
        assertFalse(iter().kthLargest(1).isPresent());
    }

    @Test
    public void testSum() {
        assertEquals(10, iter((byte) 1, (byte) 2, (byte) 3, (byte) 4).sum());
        assertEquals(0, iter().sum());
    }

    @Test
    public void testAverage_Present() {
        OptionalDouble avg = iter((byte) 2, (byte) 4).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_Empty() {
        assertFalse(iter().average().isPresent());
    }

    @Test
    public void testSummaryStatistics() {
        ByteSummaryStatistics stats = iter((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals((byte) 1, stats.getMin());
        assertEquals((byte) 5, stats.getMax());
        assertEquals(15, stats.getSum());
    }

    @Test
    public void testAnyMatch_True() {
        assertTrue(iter((byte) 1, (byte) 2, (byte) 3).anyMatch(b -> b > 2));
    }

    @Test
    public void testAnyMatch_False() {
        assertFalse(iter((byte) 1, (byte) 2, (byte) 3).anyMatch(b -> b > 10));
    }

    @Test
    public void testAllMatch_True() {
        assertTrue(iter((byte) 1, (byte) 2, (byte) 3).allMatch(b -> b > 0));
    }

    @Test
    public void testAllMatch_False() {
        assertFalse(iter((byte) 1, (byte) 2, (byte) 3).allMatch(b -> b > 1));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(iter((byte) 1, (byte) 2, (byte) 3).noneMatch(b -> b > 10));
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(iter((byte) 1, (byte) 2, (byte) 3).noneMatch(b -> b > 2));
    }

    @Test
    public void testFindFirst_WithPredicate_Found() {
        OptionalByte result = iter((byte) 3, (byte) 1, (byte) 4, (byte) 2).findFirst(b -> b < 2);
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testFindFirst_WithPredicate_NotFound() {
        assertFalse(iter((byte) 3, (byte) 1, (byte) 4, (byte) 2).findFirst(b -> b > 10).isPresent());
    }

    @Test
    public void testFindLast_WithPredicate_Found() {
        OptionalByte result = iter((byte) 1, (byte) 5, (byte) 2, (byte) 3).findLast(b -> b < 3);
        assertTrue(result.isPresent());
        assertEquals((byte) 2, result.get());
    }

    @Test
    public void testFindLast_WithPredicate_NotFound() {
        assertFalse(iter((byte) 1, (byte) 2, (byte) 3).findLast(b -> b > 10).isPresent());
    }

    @Test
    public void testAppendIfEmpty_NotEmpty() {
        byte[] result = iter((byte) 1).appendIfEmpty((byte) 99).toArray();
        assertEquals(1, result.length);
        assertEquals((byte) 1, result[0]);
    }

    @Test
    public void testAppendIfEmpty_Empty() {
        byte[] result = iter().appendIfEmpty((byte) 99).toArray();
        assertEquals(1, result.length);
        assertEquals((byte) 99, result[0]);
    }

    @Test
    public void testIfEmpty_NotTriggered() {
        boolean[] called = { false };
        iter((byte) 1).ifEmpty(() -> called[0] = true).count();
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
        assertFalse(iter((byte) 1, (byte) 2).isEmpty());
    }

    @Test
    public void testIsEmpty_True() {
        assertTrue(iter().isEmpty());
    }

    @Test
    public void testKthLargest_SortedFastPath() {
        byte[] data = { 1, 2, 3, 4, 5 };
        // sorted ascending: kthLargest(1) = last element (5)
        assertEquals((byte) 5, iter(data).sorted().kthLargest(1).get());
        // kthLargest(N) = first element (1)
        assertEquals((byte) 1, iter(data).sorted().kthLargest(5).get());
        // middle
        assertEquals((byte) 3, iter(data).sorted().kthLargest(3).get());
        // kthLargest(N+1) = empty
        assertFalse(iter(data).sorted().kthLargest(6).isPresent());
        // result must match non-sorted path on the same data
        assertEquals(iter(data).kthLargest(2).get(), iter(data).sorted().kthLargest(2).get());
    }

    @Test
    public void testKthLargestHugeRankDoesNotPreallocateRequestedCapacity() {
        assertFalse(iter((byte) 1).sorted().kthLargest(Integer.MAX_VALUE).isPresent());
    }

    @Test
    public void testSkipIteratorAdvanceIgnoresNonPositiveCounts() {
        final int[] seen = { 0 };
        final ByteStream stream = iter((byte) 1, (byte) 2, (byte) 3).onEach(value -> seen[0]++).skip(2);
        final ByteIteratorEx iterator = stream.iteratorEx();

        iterator.advance(0);
        iterator.advance(-1);
        assertEquals(0, seen[0]);
        assertEquals((byte) 3, iterator.nextByte());
        assertEquals(3, seen[0]);
        stream.close();
    }
}
