package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ContinuableFuture;

/**
 * Pinning tests (2026-09-25, fixer F10a) for the object-stream implementation:
 * <ul>
 * <li>U18-01: {@code groupJoin(Stream b, ..)} lets the primary failure win over a failing close handler of {@code b}
 * (the close failure is suppressed), like the other joins - 4-arg and 5-arg (Collector) overloads, sequential and
 * parallel left side.</li>
 * <li>U18-03: {@code combinations()} / {@code combinations(int)} on an iterator-backed source no longer close the source
 * during the intermediate call; a negative length is an {@code IllegalArgumentException} on both source kinds.</li>
 * <li>U09-01: an {@code Error} thrown by a close handler of a {@code runAsync}/{@code callAsync} task that is cancelled
 * before it started is logged, not thrown out of {@code Future.cancel()}.</li>
 * </ul>
 */
@Tag("unit")
public class StreamReview20260925Test extends TestBase {

    // ---------------------------------------------------------------------------------------------------------
    // U18-01 groupJoin(Stream b): primary failure wins over a failing close handler of b.
    // ---------------------------------------------------------------------------------------------------------

    private static Integer rightKeyFailingOn2(final Integer y) {
        if (y == 2) {
            throw new IllegalArgumentException("keyX");
        }

        return y;
    }

    private static Stream<Integer> failingCloseB(final AtomicInteger bClosed) {
        return Stream.of(1, 2, 3).onClose(() -> {
            bClosed.incrementAndGet();
            throw new IllegalStateException("closeB");
        });
    }

    private static void assertGroupJoinPrimaryWins(final Function<Stream<Integer>, Stream<String>> joinWithB, final String label) {
        final AtomicInteger bClosed = new AtomicInteger();
        final Stream<Integer> b = failingCloseB(bClosed);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> joinWithB.apply(b).toList(), label);
        assertEquals("keyX", e.getMessage(), label);
        assertEquals(1, e.getSuppressed().length, label);
        assertTrue(e.getSuppressed()[0] instanceof IllegalStateException, label);
        assertEquals("closeB", e.getSuppressed()[0].getMessage(), label);
        assertEquals(1, bClosed.get(), label);
        assertTrue(b.isClosed(), label);
    }

    @Test
    public void testU1801_groupJoinStreamB_primaryFailureWinsOverFailingCloseOfB_sequential() {
        assertGroupJoinPrimaryWins(
                b -> Stream.of(1, 2).groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, (l, r) -> l + "=" + r), "4-arg array");
        assertGroupJoinPrimaryWins(b -> Stream.of(1, 2)
                .groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, java.util.stream.Collectors.counting(), (l, r) -> l + "=" + r),
                "5-arg array");
        assertGroupJoinPrimaryWins(b -> Stream.of(Arrays.asList(1, 2).iterator())
                .groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, (l, r) -> l + "=" + r), "4-arg iterator");
        assertGroupJoinPrimaryWins(b -> Stream.of(Arrays.asList(1, 2).iterator())
                .groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, java.util.stream.Collectors.counting(), (l, r) -> l + "=" + r),
                "5-arg iterator");
    }

    @Test
    public void testU1801_groupJoinStreamB_primaryFailureWinsOverFailingCloseOfB_parallel() {
        // One left element: the right side is built exactly once, so the outcome does not depend on which worker
        // reaches the (synchronized) build first; the parallel path is still the one taken (isParallel() -> true).
        assertGroupJoinPrimaryWins(b -> Stream.of(1).parallel(2).groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, (l, r) -> l + "=" + r),
                "4-arg parallel");
        assertGroupJoinPrimaryWins(b -> Stream.of(1)
                .parallel(2)
                .groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, java.util.stream.Collectors.counting(), (l, r) -> l + "=" + r),
                "5-arg parallel");
        assertGroupJoinPrimaryWins(b -> Stream.of(Arrays.asList(1).iterator())
                .parallel(2)
                .groupJoin(b, x -> x, StreamReview20260925Test::rightKeyFailingOn2, (l, r) -> l + "=" + r), "4-arg parallel iterator");
    }

    @Test
    public void testU1801_groupJoinStreamB_closeFailureAlonePropagates_andSuccessPathUnchanged() {
        // No primary failure: the close failure of b is the failure, exactly once, nothing suppressed.
        final AtomicInteger bClosed = new AtomicInteger();
        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Stream.of(1, 2).groupJoin(failingCloseB(bClosed), x -> x, y -> y, (l, r) -> l + "=" + r).toList());
        assertEquals("closeB", e.getMessage());
        assertEquals(0, e.getSuppressed().length);
        assertEquals(1, bClosed.get());

        // Success path: result unchanged, b closed exactly once.
        final AtomicInteger closed = new AtomicInteger();
        assertEquals(Arrays.asList("1=[1]", "2=[2]"),
                Stream.of(1, 2).groupJoin(Stream.of(1, 2, 3).onClose(closed::incrementAndGet), x -> x, y -> y, (l, r) -> l + "=" + r).toList());
        assertEquals(1, closed.get());

        final AtomicInteger closed2 = new AtomicInteger();
        assertEquals(Arrays.asList("1=1", "2=1"),
                Stream.of(1, 2)
                        .groupJoin(Stream.of(1, 2, 3).onClose(closed2::incrementAndGet), x -> x, y -> y, java.util.stream.Collectors.counting(),
                                (l, r) -> l + "=" + r)
                        .toList());
        assertEquals(1, closed2.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // U18-03 combinations on an iterator-backed source: no terminal inside the intermediate call.
    // ---------------------------------------------------------------------------------------------------------

    private static Stream<Integer> iteratorSource(final List<String> events) {
        return Stream.of(Arrays.asList(1, 2, 3).iterator()).onClose(() -> events.add("closed"));
    }

    @Test
    public void testU1803_combinationsLen_iteratorSource_closesOnlyWithTheResult() {
        for (int len = 0; len <= 3; len++) {
            final List<String> events = new ArrayList<>();
            final Stream<List<Integer>> s = iteratorSource(events).combinations(len);
            events.add("after-call");

            assertEquals(Stream.of(1, 2, 3).combinations(len).toList(), s.toList(), "len=" + len);
            assertEquals(Arrays.asList("after-call", "closed"), events, "len=" + len);
        }

        // combinations(len, false) delegates to combinations(len)
        final List<String> events = new ArrayList<>();
        final Stream<List<Integer>> s = iteratorSource(events).combinations(2, false);
        events.add("after-call");
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(1, 3), Arrays.asList(2, 3)), s.toList());
        assertEquals(Arrays.asList("after-call", "closed"), events);

        // closing the result without traversing it closes the source exactly once
        final AtomicInteger closed = new AtomicInteger();
        final Stream<List<Integer>> untraversed = Stream.of(Arrays.asList(1, 2, 3).iterator()).onClose(closed::incrementAndGet).combinations(2);
        assertEquals(0, closed.get());
        untraversed.close();
        assertEquals(1, closed.get());
    }

    @Test
    public void testU1803_combinationsAll_iteratorSource_closesOnlyWithTheResult() {
        final List<String> events = new ArrayList<>();
        final Stream<List<Integer>> s = iteratorSource(events).combinations();
        events.add("after-call");

        final List<List<Integer>> all = s.toList();
        assertEquals(8, all.size());
        assertEquals(Stream.of(1, 2, 3).combinations().toList(), all);
        assertEquals(Arrays.asList("after-call", "closed"), events);

        // the array-backed branch (pinned by testS212 for len == count) keeps the same timing
        final List<String> events2 = new ArrayList<>();
        final Stream<List<Integer>> s2 = Stream.of(1, 2, 3).onClose(() -> events2.add("closed")).combinations();
        events2.add("after-call");
        assertEquals(8, s2.toList().size());
        assertEquals(Arrays.asList("after-call", "closed"), events2);
    }

    @Test
    public void testU1803_combinationsNegativeLen_isIAE_andClosesTheStream_bothSourceKinds() {
        final List<Supplier<Stream<Integer>>> sources = Arrays.asList(() -> Stream.of(1, 2, 3), () -> Stream.of(Arrays.asList(1, 2, 3).iterator()));

        for (final Supplier<Stream<Integer>> src : sources) {
            final Stream<Integer> s1 = src.get();
            final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> s1.combinations(-1));
            assertTrue(e1.getMessage().contains("len"), e1.getMessage());
            assertTrue(s1.isClosed()); // the bare checkArgNotNegative closes the stream

            final Stream<Integer> s2 = src.get();
            assertThrows(IllegalArgumentException.class, () -> s2.combinations(-1, false));
            assertTrue(s2.isClosed());

            final Stream<Integer> s3 = src.get();
            assertThrows(IllegalArgumentException.class, () -> s3.combinations(-1, true));
            assertTrue(s3.isClosed());
        }

        // len > count on an iterator-backed source is still IndexOutOfBoundsException (from the derived array stream)
        assertThrows(IndexOutOfBoundsException.class, () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).combinations(4));
    }

    @Test
    public void testU1803_combinations_failingIteratorSource_primaryWins_andSourceIsClosed() {
        final Iterator<Integer> failing = new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < 3;
            }

            @Override
            public Integer next() {
                if (i >= 3) {
                    throw new NoSuchElementException();
                }

                if (++i == 2) {
                    throw new IllegalStateException("up boom");
                }

                return i;
            }
        };

        final RuntimeException closeX = new IllegalArgumentException("closeX");
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(failing).onClose(() -> {
            closed.incrementAndGet();
            throw closeX;
        });

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> s.combinations(2));
        assertEquals("up boom", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertSame(closeX, e.getSuppressed()[0]);
        assertEquals(1, closed.get());
        assertTrue(s.isClosed());
    }

    // ---------------------------------------------------------------------------------------------------------
    // U23-07 toArray(IntFunction) on iterator-backed streams: the generator/copy failure is the primary failure,
    // a failing close handler is suppressed (the stream used to be closed by an inner terminal before the generator).
    // ---------------------------------------------------------------------------------------------------------

    private static List<Supplier<Stream<Integer>>> iteratorBackedSources() {
        return Arrays.asList(() -> Stream.of(Arrays.asList(1, 2, 3).iterator()), () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(2),
                () -> Stream.of(1, 2, 3).map(x -> x), () -> Stream.of(Arrays.asList(1, 2, 3).iterator()).parallel(3).map(x -> x));
    }

    @Test
    public void testU2307_toArrayGenerator_iteratorBacked_generatorFailureWinsOverFailingClose() {
        int i = 0;

        for (final Supplier<Stream<Integer>> src : iteratorBackedSources()) {
            final String label = "source " + i++;

            // generator returning null -> NPE primary, close failure suppressed exactly once, closed exactly once
            final AtomicInteger closed = new AtomicInteger();
            final RuntimeException closeX = new IllegalStateException("closeX");
            final Stream<Integer> s = src.get().onClose(() -> {
                closed.incrementAndGet();
                throw closeX;
            });
            final NullPointerException e = assertThrows(NullPointerException.class, () -> s.toArray(n -> null), label);
            assertEquals("generator returned null", e.getMessage(), label);
            assertEquals(1, e.getSuppressed().length, label);
            assertSame(closeX, e.getSuppressed()[0], label);
            assertEquals(1, closed.get(), label);
            assertTrue(s.isClosed(), label);

            // wrong component type -> ArrayStoreException primary, close failure suppressed
            final AtomicInteger closed2 = new AtomicInteger();
            final RuntimeException closeX2 = new IllegalStateException("closeX");
            final Stream<Integer> s2 = src.get().onClose(() -> {
                closed2.incrementAndGet();
                throw closeX2;
            });
            final ArrayStoreException e2 = assertThrows(ArrayStoreException.class, () -> s2.toArray(String[]::new), label);
            assertEquals(1, e2.getSuppressed().length, label);
            assertSame(closeX2, e2.getSuppressed()[0], label);
            assertEquals(1, closed2.get(), label);

            // a throwing generator -> its exception is primary
            final RuntimeException closeX3 = new IllegalStateException("closeX");
            final Stream<Integer> s3 = src.get().onClose(() -> {
                throw closeX3;
            });
            final UnsupportedOperationException e3 = assertThrows(UnsupportedOperationException.class, () -> s3.toArray(n -> {
                throw new UnsupportedOperationException("gen boom");
            }), label);
            assertEquals("gen boom", e3.getMessage(), label);
            assertEquals(1, e3.getSuppressed().length, label);
            assertSame(closeX3, e3.getSuppressed()[0], label);
        }
    }

    @Test
    public void testU2307_toArrayGenerator_iteratorBacked_successPathUnchanged() {
        int i = 0;

        for (final Supplier<Stream<Integer>> src : iteratorBackedSources()) {
            final String label = "source " + i++;

            // success with a good handler: result, closed exactly once
            final AtomicInteger closed = new AtomicInteger();
            final Integer[] a = src.get().onClose(closed::incrementAndGet).toArray(Integer[]::new);
            assertEquals(3, a.length, label);
            assertEquals(6, a[0] + a[1] + a[2], label);
            assertEquals(1, closed.get(), label);

            // a generator that ignores its size argument is still tolerated
            final Integer[] grown = src.get().toArray(n -> new Integer[0]);
            assertEquals(3, grown.length, label);

            // success with a failing handler: the close failure still surfaces (nothing else failed), handler ran once
            final AtomicInteger closed2 = new AtomicInteger();
            final Stream<Integer> s2 = src.get().onClose(() -> {
                closed2.incrementAndGet();
                throw new IllegalStateException("closeX");
            });
            final IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> s2.toArray(Integer[]::new), label);
            assertEquals("closeX", e2.getMessage(), label);
            assertEquals(0, e2.getSuppressed().length, label);
            assertEquals(1, closed2.get(), label);
            assertTrue(s2.isClosed(), label);
        }

        // array-backed twins keep the same contract (regression guard, pinned elsewhere too)
        final RuntimeException closeX = new IllegalStateException("closeX");
        final Stream<Integer> arr = Stream.of(1, 2, 3).onClose(() -> {
            throw closeX;
        });
        final NullPointerException e = assertThrows(NullPointerException.class, () -> arr.toArray(n -> null));
        assertEquals("generator returned null", e.getMessage());
        assertSame(closeX, e.getSuppressed()[0]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // U09-01 runAsync/callAsync cancelled before start: an Error from a close handler is logged, not thrown.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testU0901_cancelledBeforeStart_errorFromCloseHandlerIsLoggedNotThrown() throws Exception {
        final ExecutorService single = Executors.newSingleThreadExecutor();
        final CountDownLatch release = new CountDownLatch(1);

        try {
            single.execute(() -> {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            final AtomicInteger handlerRuns = new AtomicInteger();
            final Stream<Integer> s = Stream.of(1, 2).onClose(() -> {
                handlerRuns.incrementAndGet();
                throw new AssertionError("close handler error");
            });
            final ContinuableFuture<Long> future = s.callAsync(Stream::count, single);

            assertTrue(future.cancel(true)); // the AssertionError is logged, not thrown out of cancel()
            assertTrue(future.isCancelled());
            assertEquals(1, handlerRuns.get());
            assertTrue(s.isClosed());

            final AtomicInteger handlerRuns2 = new AtomicInteger();
            final Stream<Integer> s2 = Stream.of(1).onClose(() -> {
                handlerRuns2.incrementAndGet();
                throw new AssertionError("close handler error");
            });
            final ContinuableFuture<Void> future2 = s2.runAsync(x -> x.count(), single);

            assertTrue(future2.cancel(false));
            assertTrue(future2.isCancelled());
            assertEquals(1, handlerRuns2.get());
            assertTrue(s2.isClosed());

            // a RuntimeException from the handler keeps behaving the same way (regression guard for the widened catch)
            final AtomicInteger handlerRuns3 = new AtomicInteger();
            final Stream<Integer> s3 = Stream.of(1).onClose(() -> {
                handlerRuns3.incrementAndGet();
                throw new IllegalStateException("close fails");
            });
            final ContinuableFuture<Long> future3 = s3.callAsync(Stream::count, single);

            assertTrue(future3.cancel(true));
            assertEquals(1, handlerRuns3.get());
            assertTrue(s3.isClosed());
            assertFalse(single.isShutdown());
        } finally {
            release.countDown();
            single.shutdownNow();
        }
    }
}
