package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.LongIterator;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
// The ring's values and close/cancellation behaviour are covered in the regular suite by PrimitiveBufferLifecycleReview20261010Test.
/** Deterministic reachability checks: completed rings must not keep their worker sources or futures alive. */
@Tag("slow-test")
class PrimitiveBufferRetentionReview20261010Test extends TestBase {
    private static LongIteratorEx ring(final List<LongIterator> sources, final boolean cancel, final Executor executor) throws Exception {
        final Class<?> type = Class.forName(ParallelPrimitiveBuffer.class.getName() + "$PrimitiveRing");
        final Constructor<?> constructor = type.getDeclaredConstructor(List.class, boolean.class, AsyncExecutor.class);
        constructor.setAccessible(true);
        return (LongIteratorEx) constructor.newInstance(new ArrayList<>(sources), cancel, new AsyncExecutor(executor));
    }

    private static List<?> retained(final LongIteratorEx ring, final String name) throws Exception {
        final Field field = ring.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (List<?>) field.get(ring);
    }

    private static int retainedCapacity(final LongIteratorEx ring) throws Exception {
        final Field field = ring.getClass().getDeclaredField("values");
        field.setAccessible(true);
        final long[] values = (long[]) field.get(ring);
        return values == null ? 0 : values.length;
    }

    private static void start(final LongIteratorEx ring) throws Exception {
        final Method method = ring.getClass().getDeclaredMethod("start");
        method.setAccessible(true);
        method.invoke(ring);
    }

    @Test
    void exhaustionReleasesWorkerSourcesAndCompletedFutures() throws Exception {
        for (final boolean cancel : new boolean[] { false, true }) {
            final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2), LongIterator.of(3)), cancel, Runnable::run);
            try {
                assertArrayEquals(new long[] { 1, 2, 3 }, ring.toArray());
                assertEquals(0, retainedCapacity(ring), "exhaustion must release the primitive handoff allocation");
                assertTrue(retained(ring, "iterators").isEmpty(), "the completed handoff must release iterator source graphs");
                if (cancel) {
                    assertTrue(retained(ring, "futures").isEmpty(), "completed futures no longer serve a cancellation purpose");
                }
                assertFalse(ring.hasNext());
            } finally {
                ring.closeResource();
            }
        }
    }

    @Test
    void closeReleasesAnEmptyStartedBuffer() throws Exception {
        final List<Runnable> queued = new ArrayList<>();
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2)), false, queued::add);
        try {
            start(ring);
            assertTrue(retainedCapacity(ring) > 0);
            ring.closeResource();
            assertEquals(0, retainedCapacity(ring));
            queued.forEach(Runnable::run);
            assertFalse(ring.hasNext());
        } finally {
            ring.closeResource();
            queued.forEach(Runnable::run);
        }
    }

    @Test
    void closeReleasesStorageWhilePreservingTheCachedLastValue() throws Exception {
        final LongIteratorEx ring = ring(List.of(LongIterator.of(7)), false, Runnable::run);
        try {
            assertTrue(ring.hasNext());
            assertTrue(retainedCapacity(ring) > 0);
            ring.closeResource();
            assertEquals(0, retainedCapacity(ring));
            assertEquals(7, ring.nextLong());
            assertFalse(ring.hasNext());
        } finally {
            ring.closeResource();
        }
    }

    @Test
    void drainingAfterCloseReleasesTheBufferWithoutLosingPublishedValues() throws Exception {
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2, 3)), false, Runnable::run);
        try {
            start(ring);
            assertTrue(retainedCapacity(ring) > 0);
            ring.closeResource();
            assertArrayEquals(new long[] { 1, 2, 3 }, ring.toArray());
            assertEquals(0, retainedCapacity(ring));
        } finally {
            ring.closeResource();
        }
    }

    @Test
    void closeBeforeStartupReleasesSourcesWithoutSubmittingWork() throws Exception {
        final AtomicInteger submissions = new AtomicInteger();
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2)), true, command -> submissions.incrementAndGet());
        ring.closeResource();
        ring.closeResource();

        assertTrue(retained(ring, "iterators").isEmpty());
        assertTrue(retained(ring, "futures").isEmpty());
        assertFalse(ring.hasNext());
        assertEquals(0, submissions.get());
    }

    @Test
    void submittedWorkersKeepTheirOwnSourcesUntilTheyRun() throws Exception {
        final List<Runnable> queued = new ArrayList<>();
        final LongIteratorEx ring = ring(List.of(LongIterator.of(1, 2), LongIterator.of(3)), true, queued::add);
        try {
            start(ring);
            assertEquals(2, queued.size());
            assertTrue(retained(ring, "iterators").isEmpty(), "submission transfers source ownership to each worker");
            queued.forEach(Runnable::run);
            assertArrayEquals(new long[] { 1, 2, 3 }, ring.toArray());
            assertTrue(retained(ring, "futures").isEmpty());
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
        active[0] = ring(List.of(source, LongIterator.of(2)), true, Runnable::run);
        try {
            assertFalse(active[0].hasNext());
            assertTrue(retained(active[0], "iterators").isEmpty());
            assertTrue(retained(active[0], "futures").isEmpty());
        } finally {
            active[0].closeResource();
        }
    }

    @Test
    void closeCancelsQueuedWorkersBeforeReleasingTheirFutures() throws Exception {
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
            final List<?> futures = new ArrayList<>(retained(ring, "futures"));
            ring.closeResource();

            assertTrue(retained(ring, "iterators").isEmpty());
            assertTrue(retained(ring, "futures").isEmpty());
            assertTrue(((ContinuableFuture<?>) futures.get(0)).isCancelled());
            queued.forEach(Runnable::run);
            assertEquals(0, reads.get());
            assertFalse(ring.hasNext());
        } finally {
            ring.closeResource();
            queued.forEach(Runnable::run);
        }
    }
}
