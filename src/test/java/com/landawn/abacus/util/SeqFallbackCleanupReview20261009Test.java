package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SeqFallbackCleanupReview20261009Test extends TestBase {

    @Test
    void bufferedCancellationClosesFallbackReturnedAfterCancellation() throws InterruptedException {
        final CountDownLatch supplierStarted = new CountDownLatch(1);
        final CountDownLatch releaseSupplier = new CountDownLatch(1);
        final CountDownLatch childObserved = new CountDownLatch(1);
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> fallback = Seq.<Integer, RuntimeException> empty().appendIfEmpty(() -> {
            supplierStarted.countDown();
            await(releaseSupplier);
            return Seq.<Integer, RuntimeException> of(2).onEach(value -> childObserved.countDown()).onClose(() -> {
                childCloses.incrementAndGet();
                childObserved.countDown();
            });
        });
        final Seq<Integer, RuntimeException> source = Seq.concat(Seq.<Integer, RuntimeException> of(1), fallback);

        try {
            assertEquals(1, source.buffered(1).onEach(value -> await(supplierStarted)).first().orElseThrow());
        } finally {
            releaseSupplier.countDown();
            source.close();
        }

        assertTrue(childObserved.await(5, TimeUnit.SECONDS), "the buffered supplier did not finish");
        assertEquals(1, childCloses.get(), "a fallback returned after cancellation must be closed");
        fallback.close();
        assertEquals(1, childCloses.get());
    }

    @Test
    void nonemptyAndUnconsumedSequencesDoNotInvokeFallbackSupplier() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final com.landawn.abacus.util.function.Supplier<Seq<Integer, RuntimeException>> supplier = () -> {
            supplierCalls.incrementAndGet();
            return Seq.of(2);
        };

        assertEquals(List.of(1), Seq.<Integer, RuntimeException> of(1).appendIfEmpty(supplier).toList());
        Seq.<Integer, RuntimeException> empty().appendIfEmpty(supplier).close();
        assertEquals(0, supplierCalls.get());
    }

    @Test
    void normalFallbackClosesOnceAndNullFallbackIsEmpty() {
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> empty()
                .appendIfEmpty(() -> Seq.<Integer, RuntimeException> of(2, 3).onClose(childCloses::incrementAndGet));

        assertEquals(List.of(2, 3), source.toList());
        assertEquals(1, childCloses.get());
        source.close();
        assertEquals(1, childCloses.get());
        assertTrue(Seq.<Integer, RuntimeException> empty().appendIfEmpty(() -> null).toList().isEmpty());
    }

    @Test
    void fallbackSuppliedAfterCloseIsClosedImmediatelyAndAppendsNothing() {
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> empty()
                .appendIfEmpty(() -> Seq.<Integer, RuntimeException> of(2).onClose(childCloses::incrementAndGet));
        final Throwables.Iterator<Integer, RuntimeException> iterator = source.iteratorEx();

        // Same ordering as a buffered reader finishing the supplier after its consumer closed the pipeline.
        source.close();

        assertFalse(iterator.hasNext());
        assertEquals(1, childCloses.get());
        source.close();
        assertEquals(1, childCloses.get());
    }

    @Test
    void supplierFailureIsRetriedByTheNextTraversalCall() {
        final AtomicInteger supplierCalls = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("transient");
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> empty().appendIfEmpty(() -> {
            if (supplierCalls.incrementAndGet() == 1) {
                throw failure;
            }

            return Seq.of(7, 8);
        });
        final Throwables.Iterator<Integer, RuntimeException> iterator = source.iteratorEx();

        assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
        assertTrue(iterator.hasNext());
        assertEquals(7, iterator.next());
        assertEquals(8, iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(2, supplierCalls.get());
    }

    @Test
    void countAndAdvanceDelegateToTheFallbackIterator() {
        final CountingIterator counted = new CountingIterator(1_000);
        assertEquals(1_000, Seq.<Integer, RuntimeException> empty().appendIfEmpty(() -> Seq.of(counted)).count());
        assertEquals(1, counted.countCalls);
        assertEquals(0, counted.nextCalls);

        final CountingIterator advanced = new CountingIterator(1_000);
        final Throwables.Iterator<Integer, RuntimeException> iterator = Seq.<Integer, RuntimeException> empty()
                .appendIfEmpty(() -> Seq.of(advanced))
                .iteratorEx();
        iterator.advance(998);
        assertEquals(998, iterator.next());
        assertEquals(999, iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(1, advanced.advanceCalls);
        assertEquals(2, advanced.nextCalls);
    }

    /**
     * A source whose bulk operations are observable, so the tests can tell delegation from element-by-element traversal.
     */
    private static final class CountingIterator extends Throwables.Iterator<Integer, RuntimeException> {
        private final int size;
        private int cursor;
        int nextCalls;
        int countCalls;
        int advanceCalls;

        CountingIterator(final int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Integer next() {
            if (cursor >= size) {
                throw new NoSuchElementException();
            }

            nextCalls++;
            return cursor++;
        }

        @Override
        public long count() {
            countCalls++;
            final long remaining = size - cursor;
            cursor = size;
            return remaining;
        }

        @Override
        public void advance(final long n) {
            advanceCalls++;
            cursor = (int) Math.min(size, cursor + Math.max(0, n));
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "timed out coordinating buffered traversal");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
