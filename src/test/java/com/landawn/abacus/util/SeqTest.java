package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;

public class SeqTest extends SeqTestSupport {

    @Test
    public void testConstructionErrorClosesEveryOwnedSourceAndPreservesIdentity() throws Exception {
        for (int variant = 0; variant < 8; variant++) {
            for (boolean cleanupRethrowsPrimary : new boolean[] { false, true }) {
                final AssertionError failure = new AssertionError("construction failed");
                final AssertionError cleanupFailure = new AssertionError("cleanup failed");
                final AtomicInteger closed = new AtomicInteger();
                final Seq<Integer, Exception> a = org.mockito.Mockito.spy(Seq.<Integer, Exception> of(1).onClose(() -> {
                    closed.incrementAndGet();
                    throw cleanupRethrowsPrimary ? failure : cleanupFailure;
                }));
                final Seq<Integer, Exception> b = Seq.<Integer, Exception> of(2).onClose(() -> {
                    closed.incrementAndGet();
                    if (cleanupRethrowsPrimary) {
                        throw cleanupFailure;
                    }
                });
                final Seq<Integer, Exception> c = Seq.<Integer, Exception> of(3).onClose(closed::incrementAndGet);
                final int selected = variant;
                final AssertionError actual;
                if (variant < 6) {
                    // Inject failure at iterator acquisition, before the new sequence can own its inputs.
                    org.mockito.Mockito.doThrow(failure).when(a).iteratorEx();
                    actual = assertThrows(AssertionError.class, () -> {
                        switch (selected) {
                            case 0 -> Seq.zip(a, b, (x, y) -> x + y);
                            case 1 -> Seq.zip(a, b, c, (x, y, z) -> x + y + z);
                            case 2 -> Seq.zip(a, b, 0, 0, (x, y) -> x + y);
                            case 3 -> Seq.zip(a, b, c, 0, 0, 0, (x, y, z) -> x + y + z);
                            case 4 -> Seq.merge(a, b, (x, y) -> MergeResult.TAKE_FIRST);
                            default -> Seq.merge(a, b, c, (x, y) -> MergeResult.TAKE_FIRST);
                        }
                    });
                } else {
                    // Append/prepend acquire no iterator during construction; fail their delegated factory.
                    try (org.mockito.MockedStatic<Seq> factories = org.mockito.Mockito.mockStatic(Seq.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
                        if (variant == 6) {
                            factories.when(() -> Seq.concat(a, b)).thenThrow(failure);
                            actual = assertThrows(AssertionError.class, () -> a.append(b));
                        } else {
                            factories.when(() -> Seq.concat(b, a)).thenThrow(failure);
                            actual = assertThrows(AssertionError.class, () -> a.prepend(b));
                        }
                    }
                }
                assertSame(failure, actual);
                assertArrayEquals(new Throwable[] { cleanupFailure }, actual.getSuppressed());
                assertEquals(variant < 6 && variant % 2 == 1 ? 3 : 2, closed.get());
                assertThrows(IllegalStateException.class, a::count);
                assertThrows(IllegalStateException.class, b::count);

                if (variant < 6 && variant % 2 == 1) {
                    assertThrows(IllegalStateException.class, c::count);
                } else {
                    // c was never handed to the failing factory, so cleanup must have left it alone.
                    assertEquals(1L, c.count(), "An unowned source must remain usable");
                }

                a.close();
                b.close();
                c.close();
                assertEquals(3, closed.get(), "Repeated close must not repeat source cleanup");
            }
        }
    }

    @Test
    public void testConstructionFailurePreservesPrimaryWhenClosingThrowsError() {
        for (int variant = 0; variant < 6; variant++) {
            AtomicInteger closed = new AtomicInteger();
            AssertionError closeFailure = new AssertionError("close failed");
            Seq<Integer, Exception> a = Seq.<Integer, Exception> of(1).onClose(() -> {
                closed.incrementAndGet();
                throw closeFailure;
            });
            Seq<Integer, Exception> b = Seq.<Integer, Exception> of(2).onClose(closed::incrementAndGet);
            Seq<Integer, Exception> c = Seq.<Integer, Exception> of(3).onClose(closed::incrementAndGet);
            int selected = variant;
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> {
                switch (selected) {
                    case 0 -> Seq.zip(a, b, null);
                    case 1 -> Seq.zip(a, b, c, null);
                    case 2 -> Seq.zip(a, b, 0, 0, null);
                    case 3 -> Seq.zip(a, b, c, 0, 0, 0, null);
                    case 4 -> Seq.merge(a, b, null);
                    default -> Seq.merge(a, b, c, null);
                }
            });
            assertArrayEquals(new Throwable[] { closeFailure }, failure.getSuppressed());
            assertEquals(variant % 2 == 0 ? 2 : 3, closed.get());
            a.close();
            b.close();
            c.close();
            assertEquals(3, closed.get());
        }

        for (boolean prepend : new boolean[] { false, true }) {
            Seq<Integer, Exception> closed = Seq.empty();
            closed.close();
            AssertionError closeFailure = new AssertionError("close failed");
            AtomicInteger closeCount = new AtomicInteger();
            Seq<Integer, Exception> suffix = Seq.<Integer, Exception> of(1).onClose(() -> {
                closeCount.incrementAndGet();
                throw closeFailure;
            });
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> {
                if (prepend) {
                    closed.prepend(suffix);
                } else {
                    closed.append(suffix);
                }
            });
            assertArrayEquals(new Throwable[] { closeFailure }, failure.getSuppressed());
            suffix.close();
            assertEquals(1, closeCount.get());
        }
    }

    @Test
    public void testSlidingCountPreservesExactCountWhenPrefixPlusRemainingExceedsLongMaxValue() throws Exception {
        for (final int[] settings : new int[][] { { 2, 1 }, { 3, 1 }, { 3, 2 }, { 5, 3 } }) {
            final int windowSize = settings[0];
            final int increment = settings[1];
            final long expected = java.math.BigInteger.valueOf(Long.MAX_VALUE)
                    .add(java.math.BigInteger.valueOf(increment - 1))
                    .divide(java.math.BigInteger.valueOf(increment))
                    .longValueExact();

            for (boolean collector : new boolean[] { false, true }) {
                Throwables.Iterator<Integer, Exception> source = new Throwables.Iterator<>() {
                    private int cursor;
                    private java.math.BigInteger remaining = java.math.BigInteger.valueOf(Long.MAX_VALUE).add(java.math.BigInteger.valueOf(windowSize));

                    @Override
                    public boolean hasNext() {
                        return remaining.signum() > 0;
                    }

                    @Override
                    public Integer next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        remaining = remaining.subtract(java.math.BigInteger.ONE);
                        return ++cursor;
                    }

                    @Override
                    public long count() {
                        final long result = remaining.longValueExact();
                        remaining = java.math.BigInteger.ZERO;
                        return result;
                    }
                };

                try (Seq<? extends List<Integer>, Exception> windows = collector ? Seq.of(source).sliding(windowSize, increment, Collectors.toList())
                        : Seq.of(source).sliding(windowSize, increment, ArrayList::new)) {
                    Throwables.Iterator<? extends List<Integer>, Exception> iter = windows.iteratorEx();
                    assertEquals(windowSize, iter.next().size());
                    assertEquals(expected, iter.count());
                    assertFalse(iter.hasNext());
                    assertEquals(0, iter.count());
                }
            }
        }
    }

    @Test
    public void testSkipAndLimitHonorsLongMaxValueForUnboundedSource() throws Exception {
        for (long offset : new long[] { 0, 1 }) {
            List<Long> advances = new ArrayList<>();
            Throwables.Iterator<Integer, Exception> source = new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    return 1;
                }

                @Override
                public boolean supportsFailureAtomicAdvance() {
                    return true;
                }

                @Override
                public void advance(final long n) {
                    advances.add(n);
                }
            };

            try (Seq<Integer, Exception> limited = Seq.of(source).skipAndLimit(offset, Long.MAX_VALUE)) {
                Throwables.Iterator<Integer, Exception> iter = limited.iteratorEx();
                iter.advance(Long.MAX_VALUE);
                assertFalse(iter.hasNext());
                assertThrows(NoSuchElementException.class, iter::next);
                assertEquals(offset == 0 ? List.of(Long.MAX_VALUE) : List.of(offset, Long.MAX_VALUE), advances);
            }
        }
    }

    @Test
    public void testAverageIntPreservesExtremeIntegerTotals() throws Exception {
        assertEquals(-0.5, Seq.of(Integer.MIN_VALUE, Integer.MAX_VALUE).averageInt(value -> value).getAsDouble());
        assertEquals((double) Integer.MAX_VALUE, Seq.of(Integer.MAX_VALUE, Integer.MAX_VALUE).averageInt(value -> value).getAsDouble());
        assertTrue(Seq.<Integer, Exception> empty().averageInt(value -> value).isEmpty());
    }

    @Test
    public void testGroupingAccumulatesBeforeReadingFollowingElement() throws Exception {
        for (boolean intermediate : new boolean[] { false, true }) {
            AtomicInteger read = new AtomicInteger();
            AtomicInteger accumulated = new AtomicInteger();
            AtomicInteger containers = new AtomicInteger();
            Throwables.Iterator<Integer, Exception> source = new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() {
                    return read.get() < 4;
                }

                @Override
                public Integer next() {
                    assertEquals(read.get(), accumulated.get());
                    return read.incrementAndGet();
                }
            };
            java.util.stream.Collector<Integer, int[], Integer> downstream = java.util.stream.Collector.of(() -> {
                containers.incrementAndGet();
                return new int[1];
            }, (sum, value) -> {
                sum[0] += value;
                accumulated.incrementAndGet();
            }, (left, right) -> new int[] { left[0] + right[0] }, sum -> sum[0]);
            Seq<Integer, Exception> seq = Seq.of(source);
            Map<Integer, Integer> groups;
            if (intermediate) {
                Seq<Map.Entry<Integer, Integer>, Exception> grouped = seq.groupBy(value -> value % 2, downstream);
                assertEquals(0, read.get());
                groups = grouped.toMap(Map.Entry::getKey, Map.Entry::getValue);
            } else {
                groups = seq.groupTo(value -> value % 2, downstream);
            }
            assertEquals(Map.of(0, 6, 1, 4), groups);
            assertEquals(4, accumulated.get());
            assertEquals(2, containers.get());
        }

        assertEquals(Map.of(false, 0L, true, 0L), Seq.<Integer, Exception> empty().partitionTo(value -> value > 0, Collectors.counting()));
        assertEquals(Arrays.asList(Map.entry(false, 0L), Map.entry(true, 2L)), Seq.of(1, 2).partitionBy(value -> value > 0, Collectors.counting()).toList());
        assertEquals(Map.of(0, 2, 1, 2), Seq.of(1, 2, 3, 4).countBy(value -> value % 2).toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(3, Seq.of("same", "same", "same").toMultiset().getCount("same"));
    }

    @Test
    public void testIterableZipObtainsIteratorsBeforeLazyTraversal() throws Exception {
        for (int variant = 0; variant < 4; variant++) {
            AtomicInteger created = new AtomicInteger();
            AtomicInteger pulled = new AtomicInteger();
            AtomicInteger mapped = new AtomicInteger();
            Iterable<Integer> source = () -> {
                created.incrementAndGet();
                return new java.util.Iterator<>() {
                    private int remaining = 2;

                    @Override
                    public boolean hasNext() {
                        return remaining > 0;
                    }

                    @Override
                    public Integer next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        pulled.incrementAndGet();
                        return remaining--;
                    }
                };
            };
            Throwables.BiFunction<Integer, Integer, Integer, Exception> pair = (a, b) -> {
                mapped.incrementAndGet();
                return a + b;
            };
            Throwables.TriFunction<Integer, Integer, Integer, Integer, Exception> triple = (a, b, c) -> {
                mapped.incrementAndGet();
                return a + b + c;
            };
            Seq<Integer, Exception> zipped;
            switch (variant) {
                case 0:
                    zipped = Seq.zip(source, source, pair);
                    break;
                case 1:
                    zipped = Seq.zip(source, source, source, triple);
                    break;
                case 2:
                    zipped = Seq.zip(source, source, 0, 0, pair);
                    break;
                default:
                    zipped = Seq.zip(source, source, source, 0, 0, 0, triple);
                    break;
            }
            int arity = variant % 2 == 0 ? 2 : 3;
            try (zipped) {
                assertEquals(arity, created.get());
                assertEquals(0, pulled.get());
                assertEquals(0, mapped.get());
                assertEquals(Arrays.asList(2 * arity, arity), zipped.toList());
                assertEquals(2 * arity, pulled.get());
                assertEquals(2, mapped.get());
            }
        }
    }

    @Test
    public void testMapLastChecksAvailabilityWithoutPullingFollowingElement() {
        AtomicInteger pulled = new AtomicInteger();
        AtomicInteger mapped = new AtomicInteger();
        Throwables.Iterator<Integer, RuntimeException> source = new Throwables.Iterator<>() {
            private int next = 1;

            @Override
            public boolean hasNext() {
                return next <= 3;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                pulled.incrementAndGet();
                return next++;
            }
        };
        try (Seq<Integer, RuntimeException> seq = Seq.of(source).mapLast(value -> {
            mapped.incrementAndGet();
            return value * 10;
        })) {
            Throwables.Iterator<Integer, RuntimeException> iterator = seq.iteratorEx();
            assertEquals(1, iterator.next());
            assertEquals(1, pulled.get());
            assertEquals(0, mapped.get());
            assertEquals(2, iterator.next());
            assertEquals(2, pulled.get());
            assertEquals(0, mapped.get());
            assertEquals(30, iterator.next());
            assertEquals(3, pulled.get());
            assertEquals(1, mapped.get());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIfEmptyRetriesSourceInspectionAndRunsActionOnce() {
        for (boolean actionFails : new boolean[] { false, true }) {
            AtomicInteger inspections = new AtomicInteger();
            AtomicInteger actions = new AtomicInteger();
            Throwables.Iterator<Integer, RuntimeException> source = new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() {
                    if (inspections.getAndIncrement() == 0) {
                        throw new IllegalStateException("empty source inspection failed");
                    }
                    return false;
                }

                @Override
                public Integer next() {
                    throw new NoSuchElementException();
                }
            };
            try (Seq<Integer, RuntimeException> seq = Seq.of(source).ifEmpty(() -> {
                actions.incrementAndGet();
                if (actionFails) {
                    throw new IllegalArgumentException("empty action failed");
                }
            })) {
                Throwables.Iterator<Integer, RuntimeException> iterator = seq.iteratorEx();
                assertThrows(IllegalStateException.class, iterator::hasNext);
                assertEquals(0, actions.get());
                if (actionFails) {
                    assertThrows(IllegalArgumentException.class, iterator::hasNext);
                } else {
                    assertFalse(iterator.hasNext());
                }
                assertFalse(iterator.hasNext());
                assertEquals(1, actions.get());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }
    }

    @Test
    public void testSlicingRetriesDeferredSourceInitialization() {
        for (boolean nullFirst : new boolean[] { false, true }) {
            AtomicInteger attempts = new AtomicInteger();
            Throwables.Iterator<Integer, RuntimeException> deferred = Throwables.Iterator.defer(() -> {
                if (attempts.incrementAndGet() == 1) {
                    if (nullFirst) {
                        return null;
                    }
                    throw new IllegalStateException("source creation failed");
                }
                return Throwables.Iterator.of(1, 2, 3, 4);
            });
            try (Seq<Integer, RuntimeException> slice = Seq.of(deferred).skip(2).limit(1)) {
                Throwables.Iterator<Integer, RuntimeException> iterator = slice.iteratorEx();
                assertThrows(IllegalStateException.class, iterator::hasNext);
                assertEquals(Arrays.asList(3), iterator.toList());
                assertEquals(2, attempts.get());
            }
        }
    }

    @Test
    public void testSlicingPreservesBulkAdvanceAndZeroConsumptionLaziness() {
        try (Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> repeat(7, Long.MAX_VALUE);
             Seq<Integer, RuntimeException> slice = source.limit(Long.MAX_VALUE).skip(Long.MAX_VALUE - 1)) {
            Throwables.Iterator<Integer, RuntimeException> iterator = slice.iteratorEx();
            assertTrue(iterator.supportsFailureAtomicAdvance(), "huge arithmetic slices must retain bulk advancement");
            iterator.advance(1);
            assertFalse(iterator.hasNext());
        }

        try (Seq<Integer, RuntimeException> source = Seq.rangeClosed(Integer.MIN_VALUE, Integer.MAX_VALUE);
             Seq<Integer, RuntimeException> slice = source.skip(4_294_967_294L).limit(2)) {
            Throwables.Iterator<Integer, RuntimeException> iterator = slice.iteratorEx();
            assertTrue(iterator.supportsFailureAtomicAdvance());
            iterator.advance(1);
            assertEquals(Integer.MAX_VALUE, iterator.next());
            assertFalse(iterator.hasNext());
        }

        AtomicInteger initialized = new AtomicInteger();
        Throwables.Iterator<Integer, RuntimeException> deferred = Throwables.Iterator.defer(() -> {
            initialized.incrementAndGet();
            return Throwables.Iterator.of(1, 2);
        });
        try (Seq<Integer, RuntimeException> slice = Seq.of(deferred).limit(0).skip(1)) {
            slice.iteratorEx().advance(0);
            assertFalse(slice.iteratorEx().hasNext());
            assertEquals(0, initialized.get());
        }
        assertEquals(0, initialized.get());
    }

    @Test
    public void testLimitPreservesQuotaAfterSourceFailure() {
        for (boolean bulkAdvance : new boolean[] { false, true }) {
            try (Seq<Integer, RuntimeException> seq = Seq.of(failingSlicingIterator()).limit(3)) {
                Throwables.Iterator<Integer, RuntimeException> iterator = seq.iteratorEx();
                if (bulkAdvance) {
                    assertThrows(IllegalStateException.class, () -> iterator.advance(3));
                    assertEquals(Arrays.asList(2, 3), iterator.toList());
                } else {
                    assertEquals(1, iterator.next());
                    assertThrows(IllegalStateException.class, iterator::next);
                    assertEquals(Arrays.asList(2, 3), iterator.toList());
                }
            }
        }
    }

    @Test
    public void testFirstMappingWaitsForSuccessfulSourceRead() {
        for (boolean mapElse : new boolean[] { false, true }) {
            AtomicInteger attempts = new AtomicInteger();
            AtomicInteger delivered = new AtomicInteger();
            IllegalStateException failure = new IllegalStateException("source failed");
            Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
                @Override
                public boolean hasNext() {
                    return delivered.get() < 2;
                }

                @Override
                public Integer next() {
                    if (attempts.getAndIncrement() == 0) {
                        throw failure;
                    }
                    return delivered.incrementAndGet();
                }
            });

            try (Seq<Integer, RuntimeException> mapped = mapElse ? source.mapFirstOrElse(value -> value * 10, value -> value * 100)
                    : source.mapFirst(value -> value * 10)) {
                Throwables.Iterator<Integer, RuntimeException> iterator = mapped.iteratorEx();
                assertSame(failure, assertThrows(IllegalStateException.class, iterator::next));
                assertEquals(10, iterator.next());
                assertEquals(mapElse ? 200 : 2, iterator.next());
                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    public void testDropWhileContinuesDroppingAfterFailure() {
        for (boolean predicateFailure : new boolean[] { false, true }) {
            AtomicBoolean failed = new AtomicBoolean();
            AtomicInteger delivered = new AtomicInteger();
            IllegalStateException failure = new IllegalStateException("drop failed");
            Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
                @Override
                public boolean hasNext() {
                    return delivered.get() < 4;
                }

                @Override
                public Integer next() {
                    if (!predicateFailure && delivered.get() == 1 && failed.compareAndSet(false, true)) {
                        throw failure;
                    }
                    return delivered.incrementAndGet();
                }
            });

            try (Seq<Integer, RuntimeException> stream = source.dropWhile(value -> {
                if (predicateFailure && value == 2 && failed.compareAndSet(false, true)) {
                    throw failure;
                }
                return value < 3;
            })) {
                Throwables.Iterator<Integer, RuntimeException> iterator = stream.iteratorEx();
                assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
                assertEquals(Arrays.asList(3, 4), iterator.toList());
            }
        }
    }

    @Test
    public void test_empty_seq_with_multiple_onClose_still_closes_all() {
        AtomicInteger close1Count = new AtomicInteger(0);
        AtomicInteger close2Count = new AtomicInteger(0);

        Seq<Object, Exception> emptySeq = Seq.<Object, Exception> empty().onClose(close1Count::incrementAndGet).onClose(close2Count::incrementAndGet);

        emptySeq.close();

        assertEquals(1, close1Count.get());
        assertEquals(1, close2Count.get());

        emptySeq.close();
        assertEquals(1, close1Count.get());
        assertEquals(1, close2Count.get());
    }

    @Test
    public void testEmptySequenceCannotBeReusedAcrossPipelines() throws Exception {
        Seq<Integer, Exception> empty = Seq.empty();

        Assertions.assertTrue(empty.filter(n -> n > 0).toList().isEmpty());
        // Deriving again from a source that a terminal operation has already closed is rejected, rather than
        // silently answering over an exhausted iterator.
        assertThrows(IllegalStateException.class, () -> empty.map(Object::toString));
    }

    @Test
    public void testClosedStatePrecedesNullFunctionalArgUse() {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.rateLimited(0));
        assertThrows(IllegalStateException.class, () -> seq.delay((java.time.Duration) null));
        assertThrows(IllegalStateException.class, () -> seq.sortedByInt(null));
        assertThrows(IllegalStateException.class, () -> seq.hasMatchCountBetween(-1, -1, null));
        assertThrows(IllegalStateException.class, () -> seq.skipUntil(null));
        assertThrows(IllegalStateException.class, () -> seq.throwIfEmpty(null));
        assertThrows(IllegalStateException.class, () -> seq.transform(null));
        assertDoesNotThrow(seq::close);
    }

    @Test
    public void testNullFunctionalArgumentsThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).filter(null).count());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).map(null).count());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).forEach(null));
    }

    @Test
    public void testOptimizedIteratorCountExhaustsRemainingElements() throws Exception {
        final Throwables.Iterator<String, Exception> partiallyConsumed = Seq.<String, Exception> of("a", "b", "c").iteratorEx();
        assertEquals("a", partiallyConsumed.next());
        assertEquals(2, partiallyConsumed.count());
        assertFalse(partiallyConsumed.hasNext());

        assertIteratorCountExhausts(Seq.<Boolean, Exception> of(true, false), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new boolean[] { true, false }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new char[] { 'a', 'b' }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new byte[] { 1, 2 }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new short[] { 1, 2 }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new int[] { 1, 2 }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new long[] { 1, 2 }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new float[] { 1, 2 }), 2);
        assertIteratorCountExhausts(Seq.<Exception> of(new double[] { 1, 2 }), 2);

        assertIteratorCountExhausts(Seq.<Pair<Integer, Integer>, Exception> splitByChunkCount(7, 3, true, Pair::of), 3);
        assertIteratorCountExhausts(Seq.<Pair<Integer, Integer>, Exception> splitByChunkCount(7, 3, false, Pair::of), 3);
        assertIteratorCountExhausts(Seq.<Integer, Exception> of(1, 2, 3).reversed(), 3);
        assertIteratorCountExhausts(Seq.<Integer, Exception> of(1, 2, 3).rotated(1), 3);
        assertIteratorCountExhausts(Seq.<Integer, Exception> of(3, 1, 2).sorted(), 3);
    }

    @Test
    public void testConcurrentCloseHandling() throws Exception {
        boolean[] handler1Called = { false };
        boolean[] handler2Called = { false };

        Seq<String, Exception> seq = Seq.of("test").onClose(() -> {
            handler1Called[0] = true;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }).onClose(() -> handler2Called[0] = true);

        Thread t1 = new Thread(seq::close);
        Thread t2 = new Thread(seq::close);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertTrue(handler1Called[0]);
        assertTrue(handler2Called[0]);
    }

    @Test
    public void test_concat_collectionSnapshotPreservesTraversalAndClosing() throws Exception {
        AtomicInteger closeCount = new AtomicInteger();
        Seq<Integer, Exception> source = Seq.<Integer, Exception> of(1, 2).onClose(closeCount::incrementAndGet);
        List<Seq<Integer, Exception>> sources = new ArrayList<>();
        sources.add(source);

        Seq<Integer, Exception> concatenated = Seq.concat(sources);
        sources.clear();

        assertEquals(Arrays.asList(1, 2), concatenated.toList());
        assertEquals(1, closeCount.get());
    }

    @Test
    public void test_concat_closeRepeatedFailureDoesNotSkipLaterSources() {
        RuntimeException repeatedFailure = new RuntimeException("repeated close failure");
        AtomicInteger laterCloseCalls = new AtomicInteger();
        Seq<Integer, Exception> first = Seq.<Integer, Exception> of(1).onClose(() -> {
            throw repeatedFailure;
        });
        Seq<Integer, Exception> second = Seq.<Integer, Exception> of(2).onClose(() -> {
            throw repeatedFailure;
        });
        Seq<Integer, Exception> third = Seq.<Integer, Exception> of(3).onClose(laterCloseCalls::incrementAndGet);
        Seq<Integer, Exception> concatenated = Seq.concat(first, second, third);

        RuntimeException thrown = assertThrows(RuntimeException.class, concatenated::close);

        assertSame(repeatedFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
        assertEquals(1, laterCloseCalls.get());
    }

    @Test
    public void testDifferenceMapperSupportsCheckedException() {
        final java.io.IOException failure = new java.io.IOException("mapper failure");
        final Seq<Integer, java.io.IOException> seq = Seq.<Integer, java.io.IOException> of(1, 2, 3);

        final java.io.IOException thrown = assertThrows(java.io.IOException.class,
                () -> seq.difference((Throwables.Function<Integer, Integer, java.io.IOException>) value -> {
                    throw failure;
                }, Arrays.asList(1, 2)).toList());

        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }

    @Test
    public void testPeekIfBiPredicateActionSupportsCheckedException() {
        final java.io.IOException failure = new java.io.IOException("peek failure");
        final Seq<Integer, java.io.IOException> seq = Seq.<Integer, java.io.IOException> of(1, 2, 3);

        final java.io.IOException thrown = assertThrows(java.io.IOException.class,
                () -> seq.peekIf((value, index) -> true, (Throwables.Consumer<Integer, java.io.IOException>) value -> {
                    throw failure;
                }).toList());

        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }

    @Test
    public void testTopSupportsNullWhenComparatorDoes() throws Exception {
        final List<Integer> result = Seq.of(1, null, 2).top(2, Comparator.nullsLast(Comparator.naturalOrder())).toList();

        assertEquals(2, result.size());
        assertTrue(result.contains(2));
        assertTrue(result.contains(null));
    }

    @Test
    public void testTopHugeLimitDoesNotPreallocateFromLimit() throws Exception {
        List<Integer> result = Seq.of(3, 1, 2).top(Integer.MAX_VALUE, Comparator.naturalOrder()).toList();
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testEmpty() throws Exception {
        assertEquals(0, Seq.empty().count());
        assertTrue(Seq.<Integer, Exception> empty().toList().isEmpty());
        assertFalse(Seq.<Integer, Exception> empty().first().isPresent());
        assertTrue(Seq.<Integer, Exception> empty().filter(n -> n > 0).toList().isEmpty());
        assertTrue(Seq.<Integer, Exception> empty().map(Object::toString).toList().isEmpty());
        Seq<Integer, Exception> empty = Seq.empty();
        assertTrue(empty.filter(n -> n > 0).toList().isEmpty());
        assertThrows(IllegalStateException.class, () -> empty.map(Object::toString));
    }

    @Test
    public void testJust() throws Exception {
        assertEquals(Collections.singletonList("hello"), Seq.just("hello").toList());
        assertEquals(Collections.singletonList(null), Seq.just(null).toList());
        assertEquals(Collections.singletonList("hello"), Seq.just("hello", IOException.class).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.defer(null));
    }

    @Test
    public void testRepeat() throws Exception {
        assertEquals(Arrays.asList("r", "r", "r"), Seq.repeat("r", 3).toList());
        assertTrue(Seq.repeat("r", 0).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("r", -1));
    }

    @Test
    public void testRange() throws Exception {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), Seq.range(0, 5).toList());
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), Seq.range(0, 10, 2).toList());
        assertEquals(Arrays.asList(5, 3, 1), Seq.range(5, 0, -2).toList());
        assertTrue(Seq.range(1, 1).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seq.range(1, 5, 0));
        assertEquals(10000, Seq.range(0, 10000).count());
        int[] counter = { 0 };
        assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10, 12, 14, 16, 18), Seq.range(0, 1000000).takeWhile(n -> {
            counter[0]++;
            return n < 10;
        }).map(n -> n * 2).toList());
        assertTrue(counter[0] < 20);
    }

    @Test
    public void testRangeClosed() throws Exception {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), Seq.rangeClosed(0, 5).toList());
        assertEquals(Collections.singletonList(1), Seq.rangeClosed(1, 1).toList());
        assertEquals(Arrays.asList(1, 3, 5), Seq.rangeClosed(1, 5, 2).toList());
        assertEquals(Arrays.asList(5, 3, 1), Seq.rangeClosed(5, 1, -2).toList());
        assertEquals(Arrays.asList(0, 3, 6, 9), Seq.rangeClosed(0, 10, 3).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.rangeClosed(1, 5, 0));
    }

    @Test
    public void testListFiles() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("listFilesRoot"));
        File sub = root.resolve("sub").toFile();
        sub.mkdir();
        File file1 = root.resolve("file1.txt").toFile();
        file1.createNewFile();
        File nested = new File(sub, "file2.txt");
        nested.createNewFile();
        List<File> nonRecursive = Seq.listFiles(root.toFile()).toList();
        assertEquals(2, nonRecursive.size());
        assertTrue(nonRecursive.contains(file1));
        assertTrue(nonRecursive.contains(sub));
        Set<String> recursive = Seq.listFiles(root.toFile(), true).map(File::getName).toSet();
        assertTrue(recursive.containsAll(Arrays.asList("file1.txt", "sub", "file2.txt")));
        assertTrue(Seq.listFiles(tempDir.resolve("missing").toFile()).toList().isEmpty());
        assertThrows(java.io.FileNotFoundException.class, () -> Seq.ofLines(new File("non_existent_file.txt")).toList());
        assertThrows(java.io.FileNotFoundException.class, () -> Seq.ofLines(Paths.get("non_existent_path.txt")).toList());
    }

    @Test
    public void testConcat() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4), Seq.concat(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.concat(new Integer[] { 1, 2, 3 }, null, new Integer[] { 4, 5 }).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d"), Seq.concat(Arrays.asList("a", "b"), Arrays.asList("c", "d")).toList());
        assertTrue(Seq.concat((Iterable<String>) null, (Iterable<String>) null).toList().isEmpty());
        assertEquals(Arrays.asList("a", "b", "c", "d"), Seq.concat(Arrays.asList("a", "b").iterator(), Arrays.asList("c", "d").iterator()).toList());
        AtomicBoolean s1Closed = new AtomicBoolean();
        AtomicBoolean s2Closed = new AtomicBoolean();
        assertEquals(Arrays.asList("a", "b", "c", "d"),
                Seq.concat(Seq.of("a", "b").onClose(() -> s1Closed.set(true)), Seq.of("c", "d").onClose(() -> s2Closed.set(true))).toList());
        assertTrue(s1Closed.get() && s2Closed.get());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), Seq.concat(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5 }, new Integer[] { 6, 7, 8 }).toList());
    }

    @Test
    public void testFilter() throws Exception {
        assertEquals(Arrays.asList(2, 4), Seq.of(1, 2, 3, 4).filter(x -> x % 2 == 0).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.of("a", null, "b").filter(s -> s != null).toList());
        List<Integer> dropped = new ArrayList<>();
        assertEquals(Arrays.asList(2, 4), Seq.of(1, 2, 3, 4).filter(x -> x % 2 == 0, dropped::add).toList());
        assertEquals(Arrays.asList(1, 3), dropped);
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.of("a", "b", "c", "d", "e").filter(s -> s.compareTo("c") >= 0).toArray(String[]::new));
    }

    @Test
    public void testTakeWhile() throws Exception {
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2, 3, 4).takeWhile(x -> x < 3).toList());
        assertArrayEquals(new String[] { "a", "b" }, Seq.of("a", "b", "c", "d").takeWhile(s -> s.compareTo("c") < 0).toArray(String[]::new));
    }

    @Test
    public void testDropWhile() throws Exception {
        assertEquals(Arrays.asList(3, 4, 5), Seq.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).dropWhile(x -> x > 10).toList());
        List<Integer> dropped = new ArrayList<>();
        assertEquals(Arrays.asList(4, 5), Seq.of(1, 2, 3, 4, 5).dropWhile(x -> x < 4, dropped::add).toList());
        assertEquals(Arrays.asList(1, 2, 3), dropped);
    }

    @Test
    public void testDistinct() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 2, 3, 1).distinct().toList());
        assertEquals(Arrays.asList("a", null, "b", "c"), Seq.of("a", null, "b", null, "c").distinct().toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 1, 2, 2, 3).distinct((a, b) -> a).toList());
        assertEquals(Arrays.asList("a", "bb"), Seq.of("a", "b", "bb", "cc").distinctBy(String::length).toList());
        assertEquals(Arrays.asList("a", "bb"), Seq.of("a", "bb", "c", "dd").distinctBy(s -> s.length() == 1 ? null : s.length()).toList());
        assertEquals(Arrays.asList("apple"), Seq.of("apple", "apricot", "avocado").distinctBy(s -> s.charAt(0), (a, b) -> a).toList());
    }

    @Test
    public void testFlatMap() throws Exception {
        assertEquals(Arrays.asList(1, 10, 2, 20), Seq.of(1, 2).flatMap(x -> Seq.of(x, x * 10)).toList());
        assertEquals(Arrays.asList(1, 2, 2, 3), Seq.of(1, 2).flatmap(x -> Arrays.asList(x, x + 1)).toList());
        assertEquals(Arrays.asList(1, 3), Seq.of(1, 2, 3).flatMap(x -> x == 2 ? null : Seq.of(x)).toList());
        assertTrue(Seq.of(1, 2).flatMap(x -> Seq.<Integer, Exception> empty()).toList().isEmpty());
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), Seq.of(1, 2, 3).flatMapArray(x -> new Integer[] { x, x * 10 }).toList());
        assertEquals(Arrays.asList(1, 3, 30, 300),
                Seq.of(1, 2, 3).flatMapArray(n -> n == 1 ? new Integer[] { n } : n == 2 ? new Integer[0] : new Integer[] { n, n * 10, n * 100 }).toList());
        assertEquals(Arrays.asList(1, 3), Seq.of(1, 2, 3).flatMapArray(n -> n == 2 ? null : new Integer[] { n }).toList());
        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        closed.close();
        assertThrows(IllegalStateException.class, () -> closed.flatMapArray(n -> new Integer[] { n }));
        assertEquals(Arrays.asList("aa", "bb"), Seq.of("a", null, "b").flatmapIfNotNull(s -> Arrays.asList(s + s)).toList());
        assertEquals(Arrays.asList("a1", "a2"), Seq.of("a", null).flatmapIfNotNull(s -> Arrays.asList(s), s -> Arrays.asList(s + "1", s + "2")).toList());
    }

    @Test
    public void testPartitionBy() throws Exception {
        Map<Boolean, List<Integer>> parts = Seq.of(1, 2, 3, 4).partitionBy(x -> x % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4), parts.get(true));
        assertEquals(Arrays.asList(1, 3), parts.get(false));
        assertEquals(2, Seq.of(1, 2, 3, 4).partitionBy(x -> x % 2 == 0, Collectors.counting()).count());
    }

    @Test
    public void testCountBy() throws Exception {
        Map<Integer, Integer> counts = Seq.of("a", "bb", "c", "dd").countBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Integer.valueOf(2), counts.get(1));
        assertEquals(Integer.valueOf(2), counts.get(2));
    }

    @Test
    public void testIntersection() throws Exception {
        assertEquals(Arrays.asList(3, 4), Seq.of(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 6)).toList());
        assertEquals(Arrays.asList("apple", "banana"), Seq.of("apple", "banana", "cherry").intersection(s -> s.length(), Arrays.asList(5, 6)).toList());
        assertEquals(Arrays.asList("a", "e"), Seq.of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).toList());
        assertEquals(Arrays.asList("cherry"), Seq.of("apple", "banana", "cherry").difference(s -> s.length(), Arrays.asList(5, 6)).toList());
        assertEquals(Arrays.asList(1, 2, 6, 7), Seq.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(3, 4, 5, 6, 7)).toList());
        assertTrue(Seq.of(1, 2).symmetricDifference(Collections.emptyList()).toList().containsAll(Arrays.asList(1, 2)));
    }

    @Test
    public void testPrepend() throws Exception {
        assertEquals(Arrays.asList(0, 1, 2), Seq.of(1, 2).prepend(0).toList());
        assertEquals(Arrays.asList(-1, 0, 1, 2), Seq.of(1, 2).prepend(-1, 0).toList());
        assertEquals(Arrays.asList("x", "y", "a", "b"), Seq.of("a", "b").prepend(Arrays.asList("x", "y")).toList());
        assertEquals(Arrays.asList(0, 1, 2), Seq.of(1, 2).prepend(Seq.of(0)).toList());
        assertEquals(Arrays.asList("world", "hello"), Seq.of("hello").prepend(Optional.of("world")).toList());
        assertEquals(Arrays.asList("hello"), Seq.of("hello").prepend(Optional.empty()).toList());
    }

    @Test
    public void testDefaultIfEmpty() throws Exception {
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).defaultIfEmpty(9).toList());
        assertEquals(Collections.singletonList(9), Seq.<Integer, Exception> empty().defaultIfEmpty(9).toList());
        assertEquals(Arrays.asList(9, 8), Seq.<Integer, Exception> empty().defaultIfEmpty(() -> Seq.of(9, 8)).toList());
        assertEquals(Arrays.asList(1), Seq.of(1).defaultIfEmpty(() -> Seq.of(9)).toList());
    }

    @Test
    public void testThrowIfEmpty() throws Exception {
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).throwIfEmpty().toList());
        assertThrows(NoSuchElementException.class, () -> Seq.empty().throwIfEmpty().toList());
        assertThrows(IllegalStateException.class, () -> Seq.<Integer, Exception> empty().throwIfEmpty(() -> new IllegalStateException("empty")).toList());
        assertEquals(Arrays.asList(1), Seq.of(1).throwIfEmpty(() -> new IllegalStateException("empty")).toList());
    }

    @Test
    public void testIfEmpty() throws Exception {
        AtomicBoolean ran = new AtomicBoolean();
        assertEquals(Arrays.asList(1), Seq.of(1).ifEmpty(() -> ran.set(true)).toList());
        assertFalse(ran.get());
        Seq.<Integer, Exception> empty().ifEmpty(() -> ran.set(true)).toList();
        assertTrue(ran.get());
    }

    @Test
    public void testPeek() throws Exception {
        List<Integer> seen = new ArrayList<>();
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).peek(seen::add).toList());
        assertEquals(Arrays.asList(1, 2, 3), seen);
        seen.clear();
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).peekFirst(seen::add).toList());
        assertEquals(Collections.singletonList(1), seen);
        seen.clear();
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).peekLast(seen::add).toList());
        assertEquals(Collections.singletonList(3), seen);
        seen.clear();
        assertEquals(Arrays.asList(1, 2, 3, 4), Seq.of(1, 2, 3, 4).peekIf(x -> x % 2 == 0, seen::add).toList());
        assertEquals(Arrays.asList(2, 4), seen);
        seen.clear();
        assertEquals(Arrays.asList(10, 20, 30, 40), Seq.of(10, 20, 30, 40).peekIf((v, i) -> i % 2 == 0, seen::add).toList());
        assertEquals(Arrays.asList(20, 40), seen);
    }

    @Test
    public void testLimit() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3, 4, 5).limit(3).toList());
        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        drainWithException(closed);
        assertThrows(IllegalStateException.class, () -> closed.limit(1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).limit(-1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).skip(-1));
    }

    @Test
    public void testLast() throws Exception {
        assertEquals(Nullable.of(3), Seq.of(1, 2, 3).last());
        assertTrue(Seq.<Integer, Exception> empty().last().isEmpty());
        assertEquals(Arrays.asList("d", "e"), Seq.of("a", "b", "c", "d", "e").last(2).toList());
    }

    @Test
    public void testTakeLast() throws Exception {
        assertEquals(Arrays.asList(4, 5), Seq.of(1, 2, 3, 4, 5).takeLast(2).toList());
        assertTrue(Seq.of(1, 2).takeLast(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).takeLast(3).toList());
    }

    @Test
    public void testTop() throws Exception {
        assertTrue(Seq.of(5, 2, 8, 1, 9, 3).top(3).toList().containsAll(Arrays.asList(5, 8, 9)));
        assertEquals(Arrays.asList(9), Seq.of(5, 2, 8, 1, 9, 3).top(1).toList());
        assertTrue(Seq.of(5, 3, 8, 1, 9, 2).top(3, Comparator.reverseOrder()).toList().containsAll(Arrays.asList(1, 2, 3)));
        assertTrue(Seq.<Integer, Exception> empty().top(3).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(3, 1, 2).top(2, null));
    }

    @Test
    public void testReversedRotatedShuffled() throws Exception {
        assertEquals(Arrays.asList(3, 2, 1), Seq.of(1, 2, 3).reversed().toList());
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), Seq.of(1, 2, 3, 4, 5).rotated(2).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2, 3, 4, 5).rotated(0).toList());
        assertEquals(5, Seq.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList().size());
        assertEquals(Seq.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList(), Seq.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).shuffled(null));
    }

    @Test
    public void testSorted() throws Exception {
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), Seq.of(3, 1, 4, 1, 5).sorted().toList());
        assertEquals(Arrays.asList(5, 4, 3, 1, 1), Seq.of(3, 1, 4, 1, 5).sorted(Comparator.reverseOrder()).toList());
        assertEquals(Arrays.asList("d", "bb", "aaa", "cccc"), Seq.of("aaa", "bb", "cccc", "d").sorted(Comparator.comparingInt(String::length)).toList());
        assertEquals(Arrays.asList("a", "bb", "ccc"), Seq.of("a", "ccc", "bb").sortedByInt(String::length).toList());
        assertEquals(Arrays.asList("a", "bb", "ccc"), Seq.of("a", "ccc", "bb").sortedByLong(s -> (long) s.length()).toList());
        assertEquals(Arrays.asList("a", "bb", "ccc"), Seq.of("a", "ccc", "bb").sortedByDouble(s -> (double) s.length()).toList());
        assertEquals(Arrays.asList("a", "bb", "ccc"), Seq.of("a", "ccc", "bb").sortedBy(String::length).toList());
        assertEquals(Arrays.asList(5, 4, 3, 1), Seq.of(3, 1, 4, 5).reverseSorted().toList());
        assertEquals(Arrays.asList("cccc", "aaa", "bb", "d"), Seq.of("aaa", "bb", "cccc", "d").reverseSortedBy(String::length).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).sorted(Comparator.naturalOrder()).sorted(Comparator.naturalOrder()).toList());
    }

    @Test
    public void testCycled() throws Exception {
        assertEquals(Arrays.asList("a", "b", "a", "b", "a"), Seq.of("a", "b").cycled().limit(5).toList());
        assertEquals(Arrays.asList("a", "b", "a", "b"), Seq.of("a", "b").cycled(2).toList());
        assertTrue(Seq.of("a").cycled(0).toList().isEmpty());
    }

    @Test
    public void testHasMatchCountBetween() throws Exception {
        assertFalse(Seq.of(1, 2, 3, 4, 5).hasMatchCountBetween(3, 3, x -> x % 2 == 0));
        assertTrue(Seq.of(2, 4, 6, 8, 10).hasMatchCountBetween(3, Long.MAX_VALUE, x -> x % 2 == 0));
    }

    @Test
    public void testBufferedIsFasterThanUnbuffered() throws Exception {
        final int millisToSleep = 50;
        final int elementCount = 6;
        long start = System.currentTimeMillis();
        Seq.range(0, elementCount).delay(Duration.ofMillis(millisToSleep)).buffered().map(it -> it * 2).delay(Duration.ofMillis(millisToSleep)).toList();
        assertTrue(System.currentTimeMillis() - start < millisToSleep * elementCount * 2);
    }

    @Test
    public void testChainedOperationsAndLifecycle() throws Exception {
        assertEquals(Arrays.asList(12, 8),
                Seq.of(1, 2, 3, 4, 5, 6).filter(x -> x % 2 == 0).map(x -> x * 2).sorted(Comparator.reverseOrder()).limit(2).toList());
        assertEquals(Arrays.asList(null, 1, null, 2, null).stream().filter(java.util.Objects::nonNull).toList(),
                Seq.of(null, 1, null, 2, null).skipNulls().toList());
        AtomicInteger closed = new AtomicInteger();
        try (Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(closed::incrementAndGet)) {
            assertEquals(3, seq.count());
        }
        assertEquals(1, closed.get());
        Seq<Integer, Exception> used = Seq.of(1, 2, 3);
        used.toList();
        assertThrows(IllegalStateException.class, used::toList);
        Throwables.Iterator<Integer, Exception> iter = Seq.of(1, 2).iteratorEx();
        iter.next();
        iter.next();
        assertThrows(NoSuchElementException.class, iter::next);
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).buffered(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).rateLimited(0.0));
    }
}
