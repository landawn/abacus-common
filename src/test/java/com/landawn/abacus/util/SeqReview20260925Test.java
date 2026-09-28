package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

/**
 * Review fixes 2026-09-25 for {@link Seq}: U10-01/U11-01 (reflection-free capacity pin for LST/C-086 and LST/C-121),
 * U10-02 (a throwing custom {@code collectionSupplier} discards the chunk/window already read; documented) and U11-02
 * (an {@code Error} thrown by a close handler during a pre-start cancellation is logged, not thrown out of
 * {@code cancel()}).
 */
public class SeqReview20260925Test extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // U10-01 / U11-01: split/sliding default ArrayList chunks and windows carry no spare capacity (C-086, C-121)
    //
    // The reflective pins in SeqReview20260924bTest / cTest are skipped under the pom's surefire argLine, which does
    // not open java.base/java.util. This pin needs no reflection: ArrayList.ensureCapacity(n) bumps modCount only
    // when it has to grow (n > backing array length), and a live iterator reports a bumped modCount as a
    // ConcurrentModificationException from its next(). So a non-empty ArrayList has exactly size() slots iff
    // ensureCapacity(size() + 1) invalidates an iterator taken before the call. Validated below against known-exact
    // and known-over-sized constructions, including an over-sizing supplier passed through the API under test.
    // ------------------------------------------------------------------------------------------------------

    /**
     * {@code true} if the backing array of {@code list} has at least one slot beyond {@code size()}. Grows the list
     * by one slot as a side effect, so probe each list once.
     */
    private static boolean hasSpareCapacity(final List<?> list) {
        final ArrayList<?> arrayList = assertInstanceOf(ArrayList.class, list);
        assertFalse(arrayList.isEmpty(), "the probe needs one element to iterate over");

        final Iterator<?> it = arrayList.iterator();
        arrayList.ensureCapacity(arrayList.size() + 1);

        try {
            it.next();
            return true; // no growth was needed: there was a spare slot
        } catch (final ConcurrentModificationException e) {
            return false; // it had to grow: the array was exactly size() long
        }
    }

    private static void assertNoSpareCapacity(final List<? extends List<?>> chunks, final String what) {
        for (int i = 0; i < chunks.size(); i++) {
            assertFalse(hasSpareCapacity(chunks.get(i)), what + ": chunk " + i + " (size " + chunks.get(i).size() + ") has spare capacity");
        }
    }

    @Test
    public void u1001_capacityProbe_agreesWithKnownConstructions() throws Exception {
        assertFalse(hasSpareCapacity(new ArrayList<>(Arrays.asList(1, 2, 3))), "copy constructor: exact");

        final ArrayList<Integer> single = new ArrayList<>(1);
        single.add(1);
        assertFalse(hasSpareCapacity(single), "exactly pre-sized");

        final ArrayList<Integer> slack = new ArrayList<>(10);
        slack.add(1);
        assertTrue(hasSpareCapacity(slack), "over-sized");

        final ArrayList<Integer> grown = new ArrayList<>(4);

        for (int i = 0; i < 5; i++) {
            grown.add(i);
        }

        assertTrue(hasSpareCapacity(grown), "grew 4 -> 6 for the fifth element");

        final ArrayList<Integer> trimmed = new ArrayList<>(10);
        trimmed.add(1);
        trimmed.trimToSize();
        assertFalse(hasSpareCapacity(trimmed), "trimToSize() - what the fix does");

        // the same probe through the API under test: a custom supplier that over-sizes IS detected
        final List<ArrayList<Integer>> overSized = Seq.<Exception> range(0, 3).split(2, (IntFunction<ArrayList<Integer>>) n -> new ArrayList<>(n + 5)).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2)), overSized);
        assertTrue(hasSpareCapacity(overSized.get(0)));
        assertTrue(hasSpareCapacity(overSized.get(1)));

        final List<ArrayList<Integer>> exactWindows = Seq.<Exception> range(0, 3).sliding(2, 1, (IntFunction<ArrayList<Integer>>) ArrayList::new).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(1, 2)), exactWindows);
        assertNoSpareCapacity(exactWindows, "sliding(2, 1, ArrayList::new)");
    }

    @Test
    public void u1001_split_defaultListChunksHaveNoSpareCapacity() throws Exception {
        // a short only chunk: chunk sizes below, at and above the 65536 pre-size bound (C-121)
        for (final int chunkSize : new int[] { 4, 1000, 1 << 16, (1 << 16) + 1, 100_000 }) {
            final List<List<Integer>> split = Seq.<Exception> range(0, 3).split(chunkSize).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), split);
            assertNoSpareCapacity(split, "split(" + chunkSize + ") over 3");
        }

        // chunks of one: every chunk is full and exact
        final List<List<Integer>> ones = Seq.<Exception> range(0, 3).split(1).toList();
        assertEquals(Arrays.asList(Arrays.asList(0), Arrays.asList(1), Arrays.asList(2)), ones);
        assertNoSpareCapacity(ones, "split(1) over 3");

        // full chunks below, at and above the bound (C-086), each followed by a short last chunk (C-121)
        for (final int chunkSize : new int[] { 2, 3, 1000, 1 << 16, (1 << 16) + 1, 100_000 }) {
            final int total = chunkSize <= 3 ? 2 * chunkSize + 1 : 150_003;
            final List<List<Integer>> chunks = Seq.<Exception> range(0, total).split(chunkSize).toList();

            assertEquals(total / chunkSize + 1, chunks.size(), "split(" + chunkSize + ") over " + total);
            assertEquals(chunkSize, chunks.get(0).size());
            assertTrue(chunks.get(chunks.size() - 1).size() < chunkSize, "last chunk is short");
            assertEquals(total, chunks.stream().mapToInt(List::size).sum());
            assertNoSpareCapacity(chunks, "split(" + chunkSize + ") over " + total);
        }

        // split(int, IntFunction) with the default factory shares the path
        final List<List<Integer>> viaFactory = Seq.<Exception> range(0, 5).split(2, IntFunctions.ofList()).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3), Arrays.asList(4)), viaFactory);
        assertNoSpareCapacity(viaFactory, "split(2, IntFunctions.ofList())");
    }

    @Test
    public void u1001_sliding_defaultListWindowsHaveNoSpareCapacity() throws Exception {
        // tumbling and overlapping windows over a short source: the only window is short (C-121)
        for (final int windowSize : new int[] { 4, 1000, 1 << 16, (1 << 16) + 1, 100_000 }) {
            final List<List<Integer>> tumbling = Seq.<Exception> range(0, 3).sliding(windowSize, windowSize).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), tumbling);
            assertNoSpareCapacity(tumbling, "sliding(" + windowSize + ", " + windowSize + ") over 3");

            final List<List<Integer>> overlapping = Seq.<Exception> range(0, 3).sliding(windowSize, 1).toList();
            assertEquals(Arrays.asList(Arrays.asList(0, 1, 2)), overlapping);
            assertNoSpareCapacity(overlapping, "sliding(" + windowSize + ", 1) over 3");
        }

        // full tumbling windows below, at and above the bound, then a short last one (C-086 / C-121)
        for (final int windowSize : new int[] { 1000, 1 << 16, (1 << 16) + 1, 100_000 }) {
            final List<List<Integer>> windows = Seq.<Exception> range(0, 150_003).sliding(windowSize, windowSize).toList();
            assertEquals(150_003 / windowSize + 1, windows.size());
            assertEquals(windowSize, windows.get(0).size());
            assertTrue(windows.get(windows.size() - 1).size() < windowSize, "last window is short");
            assertEquals(150_003, windows.stream().mapToInt(List::size).sum());
            assertNoSpareCapacity(windows, "sliding(" + windowSize + ", " + windowSize + ") over 150003");
        }

        // overlapping windows: full ones and a short final one
        final List<List<Integer>> overlapping = Seq.<Exception> range(0, 6).sliding(5, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1, 2, 3, 4), Arrays.asList(2, 3, 4, 5)), overlapping);
        assertNoSpareCapacity(overlapping, "sliding(5, 2) over 6");

        // gapped windows (increment > windowSize): the last one is short
        final List<List<Integer>> gapped = Seq.<Exception> range(0, 7).sliding(2, 3).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(3, 4), Arrays.asList(6)), gapped);
        assertNoSpareCapacity(gapped, "sliding(2, 3) over 7");

        // sliding(int) and sliding(int, IntFunction) with the default factory share the path
        final List<List<Integer>> unit = Seq.<Exception> range(0, 4).sliding(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1, 2), Arrays.asList(1, 2, 3)), unit);
        assertNoSpareCapacity(unit, "sliding(3) over 4");

        final List<List<Integer>> viaFactory = Seq.<Exception> range(0, 4).sliding(3, IntFunctions.ofList()).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1, 2), Arrays.asList(1, 2, 3)), viaFactory);
        assertNoSpareCapacity(viaFactory, "sliding(3, IntFunctions.ofList()) over 4");
    }

    // ------------------------------------------------------------------------------------------------------
    // U10-02: a custom collectionSupplier is asked only after the chunk/window has been read, so a supplier that
    // throws discards those elements and the next call continues with the following chunk/window (documented in
    // the @param of split(int, IntFunction), sliding(int, IntFunction) and sliding(int, int, IntFunction))
    // ------------------------------------------------------------------------------------------------------

    private static IntFunction<ArrayList<Integer>> throwingOnce(final AtomicInteger calls) {
        return n -> {
            if (calls.getAndIncrement() == 0) {
                throw new IllegalStateException("supplier boom");
            }

            return new ArrayList<>(n);
        };
    }

    @Test
    public void u1002_split_throwingSupplierDiscardsTheChunkAlreadyRead() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        try (Stream<ArrayList<Integer>> chunks = Seq.<Exception> range(1, 11).split(3, throwingOnce(calls)).stream()) {
            final Iterator<ArrayList<Integer>> it = chunks.iterator();

            final IllegalStateException e = assertThrows(IllegalStateException.class, it::next);
            assertEquals("supplier boom", e.getMessage());
            assertEquals(Arrays.asList(4, 5, 6), it.next(), "1, 2, 3 had been read before the supplier threw and are gone");
            assertEquals(Arrays.asList(7, 8, 9), it.next());
            assertEquals(Arrays.asList(10), it.next());
            assertFalse(it.hasNext());
            assertEquals(4, calls.get(), "asked once per chunk, the failed one included");
        }

        // an empty source never asks the supplier, so it cannot throw
        final AtomicInteger untouched = new AtomicInteger();
        assertEquals(0, Seq.<Integer, Exception> empty().split(3, throwingOnce(untouched)).count());
        assertEquals(0, untouched.get());
    }

    @Test
    public void u1002_sliding_throwingSupplierDiscardsTheWindowAlreadyRead() throws Exception {
        final AtomicInteger calls = new AtomicInteger();

        try (Stream<ArrayList<Integer>> windows = Seq.<Exception> range(1, 7).sliding(3, 1, throwingOnce(calls)).stream()) {
            final Iterator<ArrayList<Integer>> it = windows.iterator();

            final IllegalStateException e = assertThrows(IllegalStateException.class, it::next);
            assertEquals("supplier boom", e.getMessage());
            assertEquals(Arrays.asList(2, 3, 4), it.next(), "the window [1, 2, 3] was read and the overlap advanced");
            assertEquals(Arrays.asList(3, 4, 5), it.next());
            assertEquals(Arrays.asList(4, 5, 6), it.next());
            assertFalse(it.hasNext());
            assertEquals(4, calls.get());
        }

        final AtomicInteger calls2 = new AtomicInteger();

        try (Stream<ArrayList<Integer>> windows = Seq.<Exception> range(1, 7).sliding(3, throwingOnce(calls2)).stream()) {
            final Iterator<ArrayList<Integer>> it = windows.iterator();

            assertThrows(IllegalStateException.class, it::next);
            assertEquals(Arrays.asList(2, 3, 4), it.next(), "sliding(int, IntFunction) delegates to the same iterator");
        }

        // tumbling windows: the following window starts after the discarded one
        final AtomicInteger calls3 = new AtomicInteger();

        try (Stream<ArrayList<Integer>> windows = Seq.<Exception> range(1, 8).sliding(3, 3, throwingOnce(calls3)).stream()) {
            final Iterator<ArrayList<Integer>> it = windows.iterator();

            assertThrows(IllegalStateException.class, it::next);
            assertEquals(Arrays.asList(4, 5, 6), it.next());
            assertEquals(Arrays.asList(7), it.next());
            assertFalse(it.hasNext());
        }

        // a supplier that does not throw sees every element exactly once, in order
        final List<ArrayList<Integer>> ok = Seq.<Exception> range(1, 7).sliding(3, 1, (IntFunction<ArrayList<Integer>>) ArrayList::new).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5), Arrays.asList(4, 5, 6)), ok);
    }

    // ------------------------------------------------------------------------------------------------------
    // U11-02: a close handler that throws an Error during a pre-start cancellation is logged like an exception;
    // cancel() keeps reporting the successful cancellation and the sequence is closed (C-127 done() hook, now in
    // line with closeAfterFailure, which retains an Error too)
    // ------------------------------------------------------------------------------------------------------

    /** Occupies the single thread of {@code executor} until the returned latch is counted down. */
    private static CountDownLatch block(final ExecutorService executor) throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();

            try {
                release.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(started.await(10, TimeUnit.SECONDS));
        return release;
    }

    private static void drain(final ExecutorService executor, final CountDownLatch release) throws InterruptedException {
        release.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void u1102_callAsync_cancelledBeforeStart_errorFromCloseHandlerIsLoggedNotThrownOutOfCancel() throws Exception {
        for (final boolean mayInterrupt : new boolean[] { false, true }) {
            final ExecutorService single = Executors.newSingleThreadExecutor();

            try {
                final CountDownLatch release = block(single);
                final AtomicInteger handlerRuns = new AtomicInteger();
                final AtomicInteger actionRuns = new AtomicInteger();
                final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3).onClose(() -> {
                    handlerRuns.incrementAndGet();
                    throw new AssertionError("close error");
                });

                final ContinuableFuture<Long> future = seq.callAsync(s -> {
                    actionRuns.incrementAndGet();
                    return s.count();
                }, single);

                assertTrue(future.cancel(mayInterrupt), "cancel() reports the cancellation; the Error is logged");
                assertTrue(future.isCancelled());
                assertEquals(1, handlerRuns.get(), "closed by the cancellation itself");
                assertThrows(IllegalStateException.class, seq::count, "the sequence is closed");
                assertThrows(java.util.concurrent.CancellationException.class, future::get);

                drain(single, release);

                assertEquals(0, actionRuns.get(), "the action never ran");
                assertEquals(1, handlerRuns.get(), "closed exactly once");
            } finally {
                single.shutdownNow();
            }
        }
    }

    @Test
    public void u1102_runAsync_cancelledBeforeStart_errorFromCloseHandlerIsLoggedNotThrownOutOfCancel() throws Exception {
        final ExecutorService single = Executors.newSingleThreadExecutor();

        try {
            final CountDownLatch release = block(single);
            final AtomicInteger handlerRuns = new AtomicInteger();
            final AtomicInteger actionRuns = new AtomicInteger();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1).onClose(() -> {
                handlerRuns.incrementAndGet();
                throw new LinkageError("close error"); // a different Error subclass than the callAsync twin
            });

            final ContinuableFuture<Void> future = seq.runAsync(s -> {
                actionRuns.incrementAndGet();
                s.forEach(Fn.emptyConsumer());
            }, single);

            assertTrue(future.cancel(false));
            assertTrue(future.isCancelled());
            assertEquals(1, handlerRuns.get());
            assertThrows(IllegalStateException.class, seq::count);

            drain(single, release);

            assertEquals(0, actionRuns.get());
            assertEquals(1, handlerRuns.get());
        } finally {
            single.shutdownNow();
        }
    }
}
