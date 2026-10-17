package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.LongIterator;

/**
 * Values, startup and close/cancellation behaviour of {@code ParallelPrimitiveBuffer}'s ring. Whether the ring
 * releases its worker sources and futures is checked separately by the slow-test class
 * PrimitiveBufferRetentionReview20261010Test.
 */
class PrimitiveBufferLifecycleReview20261010Test extends TestBase {
    private static LongIteratorEx ring(final List<LongIterator> sources, final boolean cancel, final Executor executor) throws Exception {
        final Class<?> type = Class.forName(ParallelPrimitiveBuffer.class.getName() + "$PrimitiveRing");
        final Constructor<?> constructor = type.getDeclaredConstructor(List.class, boolean.class, AsyncExecutor.class);
        constructor.setAccessible(true);
        return (LongIteratorEx) constructor.newInstance(new ArrayList<>(sources), cancel, new AsyncExecutor(executor));
    }

    private static void start(final LongIteratorEx ring) throws Exception {
        final Method method = ring.getClass().getDeclaredMethod("start");
        method.setAccessible(true);
        method.invoke(ring);
    }

    @Test
    void exhaustionDeliversEveryWorkerValue() throws Exception {
        for (final boolean cancel : new boolean[] { false, true }) {
            final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2), LongIterator.of(3)), cancel, Runnable::run);
            try {
                assertArrayEquals(new long[] { 1, 2, 3 }, ring.toArray());
                assertFalse(ring.hasNext());
            } finally {
                ring.closeResource();
            }
        }
    }

    @Test
    void closeBeforeStartupSubmitsNoWork() throws Exception {
        final AtomicInteger submissions = new AtomicInteger();
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2)), true, command -> submissions.incrementAndGet());
        ring.closeResource();
        ring.closeResource();

        assertFalse(ring.hasNext());
        assertEquals(0, submissions.get());
    }

    @Test
    void closeKeepsTheLastValueAlreadyCachedByHasNext() throws Exception {
        for (final boolean cancel : new boolean[] { false, true }) {
            final LongIteratorEx ring = ring(List.of(LongIterator.of(7)), cancel, Runnable::run);
            try {
                assertTrue(ring.hasNext());
                ring.closeResource();
                ring.closeResource();
                assertTrue(ring.hasNext());
                assertEquals(7, ring.nextLong());
                assertFalse(ring.hasNext());
                assertThrows(NoSuchElementException.class, ring::nextLong);
            } finally {
                ring.closeResource();
            }
        }
    }

    @Test
    void workersSubmittedBeforeTheyRunStillDeliverTheirValues() throws Exception {
        final List<Runnable> queued = new ArrayList<>();
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2), LongIterator.of(3)), true, queued::add);
        try {
            start(ring);
            assertEquals(2, queued.size());
            queued.forEach(Runnable::run);
            assertArrayEquals(new long[] { 1, 2, 3 }, ring.toArray());
        } finally {
            ring.closeResource();
            queued.forEach(Runnable::run);
        }
    }

    @Test
    void inlineWorkerMayCloseDuringStartup() throws Exception {
        final LongIteratorEx[] active = new LongIteratorEx[1];
        final LongIterator source = new LongIterator() {
            @Override
            public boolean hasNext() {
                active[0].closeResource();
                return false;
            }

            @Override
            public long nextLong() {
                throw new AssertionError("closed source must not be read");
            }
        };
        // The reentrant close must stop startup before the second worker is submitted, without failing it.
        active[0] = ring(List.of(source, LongIterator.of(2)), true, Runnable::run);
        try {
            assertFalse(active[0].hasNext());
        } finally {
            active[0].closeResource();
        }
    }

    @Test
    void closeDuringWorkerReadStopsPublication() throws Exception {
        final LongIteratorEx[] active = new LongIteratorEx[1];
        final LongIterator source = new LongIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                active[0].closeResource();
                return 1;
            }
        };
        active[0] = ring(List.of(source), false, Runnable::run);
        try {
            assertFalse(active[0].hasNext());
            assertFalse(active[0].hasNext());
        } finally {
            active[0].closeResource();
        }
    }

    @Test
    void closeCancelsQueuedWorkersBeforeTheyReadTheirSource() throws Exception {
        final List<Runnable> queued = new ArrayList<>();
        final AtomicInteger reads = new AtomicInteger();
        final LongIterator source = new LongIterator() {
            @Override
            public boolean hasNext() {
                reads.incrementAndGet();
                return true;
            }

            @Override
            public long nextLong() {
                return 1;
            }
        };
        final LongIteratorEx ring = ring(List.of(source), true, queued::add);
        try {
            start(ring);
            ring.closeResource();
            queued.forEach(Runnable::run);

            assertEquals(0, reads.get());
            assertFalse(ring.hasNext());
        } finally {
            ring.closeResource();
            queued.forEach(Runnable::run);
        }
    }
}
