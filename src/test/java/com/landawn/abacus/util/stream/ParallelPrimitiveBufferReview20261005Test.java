package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.IntIterator;

/**
 * Code review 2026-10-05 of the primitive parallel handoff ({@code ParallelPrimitiveBuffer.PrimitiveRing}).
 *
 * <p>The ring replaced {@code Stream.parallelConcatIterators(...)}, whose {@code flatMap} wrapper closed the buffered
 * iterator as soon as it was exhausted. The ring released its futures and temporary executor only when the outer stream
 * was closed, so draining {@code iterator()} without closing the stream left a temporary executor's threads alive (its
 * core threads never time out) together with that executor's JVM shutdown hook. Exhaustion now releases them again,
 * and so does reporting a worker failure (which leaked the same way under the replaced implementation too).
 *
 * <p>{@code hasNext()} also entered the synchronized {@code start()} for every element; the started check is now a volatile
 * fast path that is set only after the workers are submitted, so the iterator may still be handed between threads.
 */
@Tag("unit")
@SuppressWarnings("deprecation") // iterator() is deprecated because it never closes the stream: exactly the scenario under test
class ParallelPrimitiveBufferReview20261005Test extends TestBase {

    private static final long THREAD_EXIT_TIMEOUT_MILLIS = 10_000;

    /**
     * Runs {@code action} while the shared stream executor reports itself busy, so {@code checkAsyncExecutor} must create
     * an owned temporary executor. Spawning {@code CORE_THREAD_POOL_SIZE} real threads instead would make the test heavy.
     */
    private static <T> T withBusySharedExecutor(final Callable<T> action) throws Exception {
        final Field field = StreamBase.class.getDeclaredField("ACTIVE_THREAD_NUM");
        field.setAccessible(true);
        final AtomicInteger activeThreadNum = (AtomicInteger) field.get(null);
        final int bump = StreamBase.CORE_THREAD_POOL_SIZE;

        activeThreadNum.addAndGet(bump);

        try {
            return action.call();
        } finally {
            activeThreadNum.addAndGet(-bump);
        }
    }

    /** Live threads of owned {@code AsyncExecutor} pools (the temporary executor is one) that did not exist before. */
    private static Set<Thread> ownedPoolThreadsStartedSince(final Set<Thread> before) {
        final Set<Thread> result = new HashSet<>();

        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().startsWith("abacus-async-") && !before.contains(thread)) {
                result.add(thread);
            }
        }

        return result;
    }

    private static Set<Thread> ownedPoolThreads() {
        return ownedPoolThreadsStartedSince(Set.of());
    }

    /** Values drained from an iterator, and the temporary-executor threads that were alive while it was being drained. */
    private record Drained(int[] values, Set<Thread> workers) {
    }

    /**
     * Drains {@code iterator} without closing its stream. The workers are captured after the first element, while the
     * executor must still be running; a released executor's threads are gone by the time the iterator is exhausted.
     */
    private static Drained drain(final IntIterator iterator, final Set<Thread> before) {
        final List<Integer> values = new ArrayList<>();
        Set<Thread> workers = Set.of();

        while (iterator.hasNext()) {
            values.add(iterator.nextInt());

            if (values.size() == 1) {
                workers = ownedPoolThreadsStartedSince(before);
            }
        }

        assertFalse(iterator.hasNext(), "an exhausted iterator stays exhausted");
        assertThrows(NoSuchElementException.class, iterator::nextInt);

        return new Drained(values.stream().mapToInt(Integer::intValue).toArray(), workers);
    }

    private static void assertAllTerminate(final Set<Thread> workers, final String what) throws InterruptedException {
        assertFalse(workers.isEmpty(), "the busy shared executor should have forced a temporary executor: " + what);

        for (final Thread thread : workers) {
            thread.join(THREAD_EXIT_TIMEOUT_MILLIS);
            assertFalse(thread.isAlive(), thread.getName() + " is still alive after " + what);
        }
    }

    private static List<IntIterator> intSources(final int sourceCount, final int valuesPerSource) {
        final List<IntIterator> result = new ArrayList<>(sourceCount);

        for (int s = 0; s < sourceCount; s++) {
            final int[] values = new int[valuesPerSource];

            for (int i = 0; i < valuesPerSource; i++) {
                values[i] = s * valuesPerSource + i;
            }

            result.add(IntIterator.of(values));
        }

        return result;
    }

    private static int[] sortedRange(final int size) {
        final int[] result = new int[size];

        for (int i = 0; i < size; i++) {
            result[i] = i;
        }

        return result;
    }

    private static int[] sorted(final int[] values) {
        final int[] result = values.clone();
        Arrays.sort(result);
        return result;
    }

    @Test
    void drainingTheIteratorWithoutClosingReleasesTheTemporaryExecutor() throws Exception {
        for (final boolean cancelUncompletedThreads : new boolean[] { false, true }) {
            final Set<Thread> before = ownedPoolThreads();

            final Drained drained = withBusySharedExecutor(() -> {
                // Deliberately never closed: exhaustion alone must release the workers' executor.
                final IntStream stream = ParallelPrimitiveBuffer.concatInt(intSources(3, 200), cancelUncompletedThreads, null);
                return drain(stream.iterator(), before);
            });

            assertArrayEquals(sortedRange(600), sorted(drained.values()));
            assertAllTerminate(drained.workers(), "draining without close (cancelUncompletedThreads=" + cancelUncompletedThreads + ")");
        }
    }

    @Test
    void drainingFloatingPointValuesWithoutClosingReleasesTheTemporaryExecutor() throws Exception {
        final Set<Thread> before = ownedPoolThreads();
        final Set<Thread> workers = new HashSet<>();

        final double[] drained = withBusySharedExecutor(() -> {
            final DoubleStream stream = ParallelPrimitiveBuffer.concatDouble(
                    List.of(DoubleIterator.of(-0.0, Double.NaN, 1.5), DoubleIterator.of(Double.NEGATIVE_INFINITY, Double.MIN_VALUE)), false, null);
            final DoubleIterator iterator = stream.iterator();
            final double[] values = new double[5];
            int count = 0;

            while (iterator.hasNext()) {
                values[count++] = iterator.nextDouble();

                if (count == 1) {
                    workers.addAll(ownedPoolThreadsStartedSince(before));
                }
            }

            assertEquals(5, count);
            return values;
        });

        Arrays.sort(drained);
        assertArrayEquals(new double[] { Double.NEGATIVE_INFINITY, -0.0, Double.MIN_VALUE, 1.5, Double.NaN }, drained);
        assertAllTerminate(workers, "draining a DoubleStream without close");
    }

    @Test
    void publicParallelPipelinesReleaseTheTemporaryExecutorWhenDrained() throws Exception {
        final int[] source = sortedRange(1_000);

        final Set<Thread> beforeMap = ownedPoolThreads();
        final Drained mapped = withBusySharedExecutor(() -> drain(IntStream.of(source).parallel(4).map(x -> x * 2).iterator(), beforeMap));

        final int[] expectedMapped = new int[source.length];

        for (int i = 0; i < source.length; i++) {
            expectedMapped[i] = i * 2;
        }

        assertArrayEquals(expectedMapped, sorted(mapped.values()));
        assertAllTerminate(mapped.workers(), "draining a parallel map without close");

        final Set<Thread> beforeFilter = ownedPoolThreads();
        final Drained filtered = withBusySharedExecutor(() -> drain(IntStream.of(source).parallel(4).filter(x -> x % 3 == 0).iterator(), beforeFilter));

        assertEquals(334, filtered.values().length);
        assertTrue(Arrays.stream(filtered.values()).allMatch(x -> x % 3 == 0));
        assertAllTerminate(filtered.workers(), "draining a parallel filter without close");
    }

    @Test
    void closingBeforeExhaustionStillStopsTheWorkersAndLaterClosesAreHarmless() throws Exception {
        final Set<Thread> before = ownedPoolThreads();

        final Set<Thread> workers = withBusySharedExecutor(() -> {
            final IntStream stream = ParallelPrimitiveBuffer.concatInt(intSources(2, 10_000), true, null);
            final IntIterator iterator = stream.iterator();

            for (int i = 0; i < 5; i++) {
                assertTrue(iterator.hasNext());
                iterator.nextInt();
            }

            final Set<Thread> running = ownedPoolThreadsStartedSince(before);
            stream.close();
            stream.close();
            return running;
        });

        assertAllTerminate(workers, "closing before exhaustion");

        final Set<Thread> beforeDrainThenClose = ownedPoolThreads();

        final Drained drained = withBusySharedExecutor(() -> {
            final IntStream stream = ParallelPrimitiveBuffer.concatInt(intSources(2, 50), false, null);
            final Drained result = drain(stream.iterator(), beforeDrainThenClose);
            // Exhaustion already released the executor; the explicit close must remain a no-op.
            stream.close();
            return result;
        });

        assertArrayEquals(sortedRange(100), sorted(drained.values()));
        assertAllTerminate(drained.workers(), "draining and then closing");
    }

    @Test
    void iteratorCanBeHandedToAnotherThreadAfterTheFastPathIsPublished() throws Exception {
        try (IntStream stream = ParallelPrimitiveBuffer.concatInt(intSources(4, 2_500), false, null)) {
            final IntIterator iterator = stream.iterator();
            final int[] values = new int[10_000];
            final int[] count = new int[1];

            for (int i = 0; i < 4_000; i++) {
                assertTrue(iterator.hasNext());
                assertTrue(iterator.hasNext(), "repeated hasNext() must not consume an element");
                values[count[0]++] = iterator.nextInt();
            }

            final Throwable[] failure = new Throwable[1];
            final Thread consumer = new Thread(() -> {
                try {
                    while (iterator.hasNext()) {
                        values[count[0]++] = iterator.nextInt();
                    }
                } catch (final Throwable e) {
                    failure[0] = e;
                }
            });

            consumer.start();
            consumer.join(60_000);

            assertFalse(consumer.isAlive(), "the second consumer never finished");
            assertNull(failure[0], "the second consumer failed");
            assertEquals(10_000, count[0]);
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::nextInt);
            assertArrayEquals(sortedRange(10_000), sorted(values));
        }
    }

    @Test
    void workerFailureIsStillReportedToTheConsumer() {
        final IntIterator failing = new IntIterator() {
            private int next;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                if (next == 3) {
                    throw new IllegalStateException("worker failure");
                }

                return next++;
            }
        };

        try (IntStream stream = ParallelPrimitiveBuffer.concatInt(List.of(failing), false, null)) {
            final IntIterator iterator = stream.iterator();

            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
                while (iterator.hasNext()) {
                    iterator.nextInt();
                }
            });

            assertEquals("worker failure", e.getMessage());
        }
    }

    /** A worker that fails after producing {@code failAfter} values, next to sources that would never end on their own. */
    private static IntIterator failingAfter(final int failAfter) {
        return new IntIterator() {
            private int next;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                if (next == failAfter) {
                    throw new IllegalStateException("worker failure");
                }

                return next++;
            }
        };
    }

    private static IntIterator endless() {
        return new IntIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return 7;
            }
        };
    }

    @Test
    void workerFailureReleasesTheTemporaryExecutorWithoutClose() throws Exception {
        for (final boolean cancelUncompletedThreads : new boolean[] { false, true }) {
            final Set<Thread> before = ownedPoolThreads();
            final Set<Thread> workers = new HashSet<>();

            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> withBusySharedExecutor(() -> {
                // Deliberately never closed: reporting the failure alone must release the workers' executor.
                final IntStream stream = ParallelPrimitiveBuffer.concatInt(List.of(failingAfter(50), endless(), endless()), cancelUncompletedThreads, null);
                final IntIterator iterator = stream.iterator();

                while (iterator.hasNext()) {
                    iterator.nextInt();

                    if (workers.isEmpty()) {
                        workers.addAll(ownedPoolThreadsStartedSince(before));
                    }
                }

                return null;
            }));

            assertEquals("worker failure", e.getMessage());
            assertAllTerminate(workers, "a worker failure without close (cancelUncompletedThreads=" + cancelUncompletedThreads + ")");
        }
    }
}
