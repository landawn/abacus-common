package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Throwables;

/**
 * Regression tests for the cycle-3 Stream.java fixes of the 2026-09-24 stream review.
 * <ul>
 *   <li><b>C-120</b> - {@code runAsync/callAsync}: a failing close handler replaced the action's own failure.</li>
 *   <li><b>C-127</b> - {@code runAsync/callAsync}: a future cancelled before its task started never closed the stream.</li>
 *   <li><b>C-129</b> - {@code ofReversed(List)} O(1) skip re-read the CURRENT list: a {@code CopyOnWriteArrayList} lost
 *       its snapshot, and a fail-fast list that shrank threw {@code IndexOutOfBoundsException} instead of
 *       {@code ConcurrentModificationException}.</li>
 *   <li><b>C-117</b> - filterWhile/takeWhile/dropWhileAddSubscriber: after a main failure the subscriber pulled the failed
 *       upstream (up to the whole rest of the source) before reporting it; also after catching the signal and retrying.</li>
 *   <li><b>C-131</b> - interval waits uninterruptibly (doc); the documented takeWhile idiom stops a consumer.</li>
 *   <li><b>C-130</b> - addSubscriber family: an independent subscriber failure was only suppressed onto the main
 *       failure, so it was lost when the caller had handled that failure and then closed the stream.</li>
 *   <li><b>G2-05 / G2-06 / H1-02</b> - merge tree tie policy, takeWhileAddSubscriber huge queue, awaitSubscriber
 *       interrupt paths, concat early close.</li>
 * </ul>
 */
@Tag("unit")
public class StreamReview20260924cTest extends TestBase {

    private static Stream<Integer> failingClose(final Stream<Integer> s, final RuntimeException closeFailure) {
        return s.onClose(() -> {
            throw closeFailure;
        });
    }

    private static Throwable causeOf(final ContinuableFuture<?> future) {
        final ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        return e.getCause();
    }

    // ============================================================== C-120: action failure wins over a close failure

    @Test
    public void testC120_runAndCallAsync_actionFailureWins_closeFailureSuppressed() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            for (int variant = 0; variant < 4; variant++) {
                final IllegalStateException closeX = new IllegalStateException("close fails");
                final IOException primary = new IOException("primary " + variant);
                final Stream<Integer> s = failingClose(Stream.of(1, 2), closeX);

                final ContinuableFuture<?> future = switch (variant) {
                    case 0 -> s.runAsync(x -> {
                        throw primary;
                    });
                    case 1 -> s.runAsync(x -> {
                        throw primary;
                    }, executor);
                    case 2 -> s.callAsync(x -> {
                        throw primary;
                    });
                    default -> s.callAsync(x -> {
                        throw primary;
                    }, executor);
                };

                final Throwable cause = causeOf(future);
                assertSame(primary, cause, "variant " + variant);
                assertEquals(List.of(closeX), Arrays.asList(primary.getSuppressed()), "variant " + variant);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testC120_terminalFailureInsideAction_andErrors() throws Exception {
        // The action's terminal operation already closed the stream with the close failure suppressed: nothing doubled.
        final IllegalStateException closeX = new IllegalStateException("close fails");
        final IllegalArgumentException boom = new IllegalArgumentException("boom");
        final Throwable cause = causeOf(failingClose(Stream.of(1, 2), closeX).runAsync(x -> x.forEach(e -> {
            throw boom;
        })));
        assertSame(boom, cause);
        assertEquals(List.of(closeX), Arrays.asList(boom.getSuppressed()));

        // An Error from the action wins too.
        final IllegalStateException closeY = new IllegalStateException("close fails");
        final AssertionError error = new AssertionError("error");
        final Throwable cause2 = causeOf(failingClose(Stream.of(1), closeY).callAsync(x -> {
            throw error;
        }));
        assertSame(error, cause2);
        assertEquals(List.of(closeY), Arrays.asList(error.getSuppressed()));
    }

    @Test
    public void testC120_onlyCloseFails_futureReportsCloseFailure_andSuccessPaths() throws Exception {
        final IllegalStateException closeX = new IllegalStateException("close fails");
        assertSame(closeX, causeOf(failingClose(Stream.of(1, 2), closeX).callAsync(Stream::count)));

        final AtomicInteger closed = new AtomicInteger();
        assertEquals(3L, Stream.of(1, 2, 3).onClose(closed::incrementAndGet).callAsync(Stream::count).get());
        assertEquals(1, closed.get());

        final List<Integer> seen = new CopyOnWriteArrayList<>();
        Stream.of(1, 2).onClose(closed::incrementAndGet).runAsync(x -> x.forEach(seen::add)).get();
        assertEquals(List.of(1, 2), seen);
        assertEquals(2, closed.get());

        // A direct executor runs the task inline.
        assertEquals(List.of("a", "b"), Stream.of("a", "b").onClose(closed::incrementAndGet).callAsync(Stream::toList, Runnable::run).get());
        assertEquals(3, closed.get());

        // Empty stream, Unicode elements.
        assertEquals(List.of(), Stream.<String> empty().callAsync(Stream::toList).get());
        assertEquals("😀中", Stream.of("😀", "中").callAsync(x -> x.join("")).get());
    }

    @Test
    public void testC120_validationAndClosedStream() {
        final Stream<Integer> s = Stream.of(1);
        s.close();
        assertThrows(IllegalStateException.class, () -> s.runAsync(x -> {
        }));
        assertThrows(IllegalStateException.class, () -> s.callAsync(x -> 1, Runnable::run));

        final AtomicInteger closed = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).onClose(closed::incrementAndGet).runAsync(null));
        assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).onClose(closed::incrementAndGet).callAsync((Throwables.Function<Stream<Integer>, Object, Exception>) null, Runnable::run));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).onClose(closed::incrementAndGet).runAsync(x -> {
        }, null));
        assertEquals(3, closed.get()); // argument checks close the stream

        // A rejecting executor: RejectedExecutionException, stream closed.
        final ExecutorService dead = Executors.newSingleThreadExecutor();
        dead.shutdown();
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> Stream.of(1).onClose(closed::incrementAndGet).callAsync(Stream::count, dead));
        assertEquals(4, closed.get());
    }

    @Test
    public void testC120_executorOverload_dependentStagesStayOnTheExecutor() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "c120-executor"));

        try {
            final AtomicReference<String> thread = new AtomicReference<>();
            Stream.of(1, 2).callAsync(Stream::count, executor).thenRunAsync(c -> thread.set(Thread.currentThread().getName())).get(10, TimeUnit.SECONDS);
            assertEquals("c120-executor", thread.get());

            final AtomicReference<String> thread2 = new AtomicReference<>();
            Stream.of(1, 2).runAsync(x -> x.count(), executor).thenRunAsync(() -> thread2.set(Thread.currentThread().getName())).get(10, TimeUnit.SECONDS);
            assertEquals("c120-executor", thread2.get());
        } finally {
            executor.shutdownNow();
        }
    }

    // ============================================================== C-127: cancelled before start -> stream closed

    @Test
    public void testC127_cancelledBeforeStart_closesStream_allExecutorOverloads() throws Exception {
        for (int variant = 0; variant < 4; variant++) {
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

                final AtomicInteger closed = new AtomicInteger();
                final AtomicBoolean actionRan = new AtomicBoolean();
                final Stream<Integer> s = Stream.of(1, 2, 3).onClose(closed::incrementAndGet);

                final ContinuableFuture<?> future = variant < 2 ? s.runAsync(x -> actionRan.set(true), single) : s.callAsync(x -> {
                    actionRan.set(true);
                    return x.count();
                }, single);

                assertTrue(future.cancel(variant % 2 == 0), "variant " + variant);
                assertEquals(1, closed.get(), "variant " + variant + ": closed by the cancellation itself");
                assertTrue(future.isCancelled());

                release.countDown();
                single.shutdown();
                assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));

                assertFalse(actionRan.get(), "variant " + variant);
                assertEquals(1, closed.get(), "variant " + variant + ": closed exactly once");
                assertThrows(IllegalStateException.class, s::count); // the stream is closed
            } finally {
                release.countDown();
                single.shutdownNow();
            }
        }
    }

    @Test
    public void testC127_cancelAll_andFailingCloseHandler_cancelStillSucceeds() throws Exception {
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
            final ContinuableFuture<Long> future = Stream.of(1, 2).onClose(() -> {
                handlerRuns.incrementAndGet();
                throw new IllegalStateException("close fails");
            }).callAsync(Stream::count, single);

            assertTrue(future.cancel(true)); // the close failure is logged, not thrown out of cancel()
            assertEquals(1, handlerRuns.get());

            final AtomicInteger closed = new AtomicInteger();
            final ContinuableFuture<Void> future2 = Stream.of(1).onClose(closed::incrementAndGet).runAsync(x -> x.count(), single);
            assertTrue(future2.cancelAll(false));
            assertEquals(1, closed.get());
        } finally {
            release.countDown();
            single.shutdownNow();
        }
    }

    @Test
    public void testC127_cancelledWhileRunning_actionClosesWhenItReturns() throws Exception {
        final ExecutorService single = Executors.newSingleThreadExecutor();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        try {
            final AtomicInteger closed = new AtomicInteger();
            final AtomicInteger closedSeenByAction = new AtomicInteger(-1);
            final ContinuableFuture<Void> future = Stream.of(1, 2).onClose(closed::incrementAndGet).runAsync(x -> {
                started.countDown();

                while (true) {
                    try {
                        if (release.await(10, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (final InterruptedException e) {
                        // keep waiting for the release
                    }
                }

                closedSeenByAction.set(closed.get());
            }, single);

            assertTrue(started.await(10, TimeUnit.SECONDS));
            assertTrue(future.cancel(false));
            assertEquals(0, closed.get(), "a started action is not closed under it");

            release.countDown();
            single.shutdown();
            assertTrue(single.awaitTermination(10, TimeUnit.SECONDS));

            assertEquals(0, closedSeenByAction.get());
            assertEquals(1, closed.get());
        } finally {
            release.countDown();
            single.shutdownNow();
        }
    }

    @Test
    public void testC127_defaultExecutor_cancelImmediately_streamClosedExactlyOnce() throws Exception {
        for (int i = 0; i < 20; i++) {
            final AtomicInteger closed = new AtomicInteger();
            final CountDownLatch closedLatch = new CountDownLatch(1);
            final ContinuableFuture<Long> future = Stream.of(1, 2, 3).onClose(() -> {
                closed.incrementAndGet();
                closedLatch.countDown();
            }).callAsync(Stream::count);

            future.cancel(false);
            assertTrue(closedLatch.await(10, TimeUnit.SECONDS));
            Thread.sleep(5);
            assertEquals(1, closed.get());
        }
    }

    // ============================================================== C-129: ofReversed skip keeps snapshot / fail-fast semantics

    @Test
    public void testC129_copyOnWriteArrayList_snapshotKeptAcrossSkip() {
        final List<Integer> cow = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> added = Stream.ofReversed(cow).skip(1);
        cow.add(0, 0);
        assertEquals(List.of(2, 1), added.toList()); // was [1, 0]

        final List<Integer> cow2 = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> cleared = Stream.ofReversed(cow2).skip(1);
        cow2.clear();
        assertEquals(List.of(2, 1), cleared.toList()); // was IndexOutOfBoundsException

        final List<Integer> cow3 = new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4, 5));
        final Stream<Integer> mixed = Stream.ofReversed(cow3);
        cow3.set(0, 100);
        cow3.remove(1);
        assertEquals(List.of(4, 3, 2, 1), mixed.skip(1).toList());

        final List<Integer> cow4 = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> counted = Stream.ofReversed(cow4);
        cow4.clear();
        assertEquals(3, counted.count());

        final List<Integer> cow5 = new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4));
        final Stream<Integer> stepped = Stream.ofReversed(cow5);
        cow5.add(9);
        assertEquals(List.of(4, 2), stepped.step(2).toList());
    }

    @Test
    public void testC129_snapshotListBehindAWrapper_andCowSubList() {
        final List<Integer> cow = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> wrapped = Stream.ofReversed(Collections.unmodifiableList(cow)).skip(1);
        cow.add(0, 0);
        assertEquals(List.of(2, 1), wrapped.toList());

        final List<Integer> cowBig = new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4, 5));
        assertEquals(List.of(3, 2), Stream.ofReversed(cowBig.subList(1, 4)).skip(1).toList());
    }

    @Test
    public void testC129_failFastListThatShrank_throwsConcurrentModificationException() {
        final List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        final Stream<Integer> cleared = Stream.ofReversed(list).skip(3);
        list.clear();
        assertThrows(ConcurrentModificationException.class, cleared::toList); // was IndexOutOfBoundsException

        final List<Integer> list2 = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        final Stream<Integer> shrunk = Stream.ofReversed(list2).skip(3);
        list2.subList(5, 10).clear();
        assertThrows(ConcurrentModificationException.class, shrunk::toList);

        final List<Integer> list3 = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> skipOne = Stream.ofReversed(list3).skip(1);
        list3.remove(2);
        assertThrows(ConcurrentModificationException.class, skipOne::toList);

        final List<Integer> vector = new Vector<>(List.of(1, 2, 3, 4, 5));
        final Stream<Integer> vectorShrunk = Stream.ofReversed(vector).skip(2);
        vector.clear();
        assertThrows(ConcurrentModificationException.class, vectorShrunk::toList);

        final List<Integer> backing = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        final Stream<Integer> unmodifiableView = Stream.ofReversed(Collections.unmodifiableList(backing)).skip(2);
        backing.clear();
        assertThrows(ConcurrentModificationException.class, unmodifiableView::toList);

        final List<Integer> grown = new ArrayList<>(List.of(1, 2, 3));
        final Stream<Integer> grownSkip = Stream.ofReversed(grown).skip(2);
        grown.add(4);
        assertThrows(ConcurrentModificationException.class, grownSkip::toList);
    }

    @Test
    public void testC129_skipValues_acrossListKinds() {
        final List<List<Integer>> lists = List.of(new ArrayList<>(List.of(1, 2, 3, 4, 5)), new Vector<>(List.of(1, 2, 3, 4, 5)), Arrays.asList(1, 2, 3, 4, 5),
                List.of(1, 2, 3, 4, 5), new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6)).subList(1, 6), List.of(0, 1, 2, 3, 4, 5, 6).subList(1, 6),
                Collections.unmodifiableList(new ArrayList<>(List.of(1, 2, 3, 4, 5))), new CopyOnWriteArrayList<>(List.of(1, 2, 3, 4, 5)),
                new java.util.LinkedList<>(List.of(1, 2, 3, 4, 5)));

        for (final List<Integer> list : lists) {
            final String kind = list.getClass().getName();
            assertEquals(List.of(4, 3, 2, 1), Stream.ofReversed(list).skip(1).toList(), kind);
            assertEquals(List.of(3, 2), Stream.ofReversed(list).skip(2).limit(2).toList(), kind);
            assertEquals(List.of(1), Stream.ofReversed(list).skip(4).toList(), kind);
            assertEquals(List.of(), Stream.ofReversed(list).skip(5).toList(), kind);
            assertEquals(List.of(), Stream.ofReversed(list).skip(Long.MAX_VALUE).toList(), kind);
            assertEquals(5, Stream.ofReversed(list).count(), kind);
            assertEquals(2, Stream.ofReversed(list).skip(3).count(), kind);
            assertEquals(List.of(5, 3, 1), Stream.ofReversed(list).step(2).toList(), kind);
            assertEquals(List.of(2, 1), Stream.ofReversed(list).skip(1).skip(1).skip(1).toList(), kind);
        }

        assertEquals(List.of(), Stream.ofReversed(new ArrayList<Integer>()).skip(1).toList());
        assertEquals(List.of(), Stream.ofReversed(List.of()).skip(3).toList());
        assertEquals(List.of("中"), Stream.ofReversed(List.of("中", "😀")).skip(1).toList());
        assertEquals(Arrays.asList((String) null), Stream.ofReversed(Arrays.asList(null, "b")).skip(1).toList());
    }

    /** The O(1) skip is kept for the lists that iterate live: Collections.nCopies and List.of are skipped without stepping. */
    @Test
    public void testC129_liveListsStillSkipInConstantTime() {
        final int size = Integer.MAX_VALUE - 8;
        assertEquals(List.of("x"), Stream.ofReversed(Collections.nCopies(size, "x")).skip(size - 1).toList());
        assertEquals(size, Stream.ofReversed(Collections.nCopies(size, "x")).count());
    }

    // ============================================================== C-130: independent subscriber failure after a handled main failure

    private static Stream<Integer> attach(final int op, final Stream<Integer> upstream,
            final Throwables.Consumer<? super Stream<Integer>, ? extends Exception> subscriber) {
        return switch (op) {
            case 0 -> upstream.addSubscriber(subscriber);
            case 1 -> upstream.filterWhileAddSubscriber(x -> x % 2 == 1, subscriber);
            case 2 -> upstream.takeWhileAddSubscriber(x -> x < 10, subscriber);
            default -> upstream.dropWhileAddSubscriber(x -> x < 10, subscriber);
        };
    }

    /** Upstream that fails with {@code boom} at element 2, only after the subscriber has failed on its own. */
    private static Stream<Integer> upstreamFailingAfter(final CountDownLatch subscriberFailed, final RuntimeException boom) {
        return Stream.of(1, 2, 3, 4).map(x -> {
            if (x == 2) {
                try {
                    subscriberFailed.await(1500, TimeUnit.MILLISECONDS); // filterWhile/takeWhile: the subscriber starts only on close
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                throw boom;
            }

            return x;
        });
    }

    @Test
    public void testC130_mainFailureHandledByCaller_closeStillThrowsIndependentSubscriberFailure() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final IllegalStateException subOwn = new IllegalStateException("sub own");
            final CountDownLatch subscriberFailed = new CountDownLatch(1);

            final Stream<Integer> main = attach(op, upstreamFailingAfter(subscriberFailed, boom), s -> {
                try {
                    throw subOwn;
                } finally {
                    subscriberFailed.countDown();
                }
            });

            final Iterator<Integer> it = main.iterator();
            Throwable handled = null;

            try {
                while (it.hasNext()) {
                    it.next();
                }
            } catch (final IllegalArgumentException e) {
                handled = e; // the caller handles the main failure itself
            }

            assertSame(boom, handled, "op " + op);

            final Throwable fromClose = assertThrows(IllegalStateException.class, main::close, "op " + op);
            assertSame(subOwn, fromClose, "op " + op);
            assertEquals(0, boom.getSuppressed().length, "op " + op);
        }
    }

    @Test
    public void testC130_terminalStillSuppressesIndependentSubscriberFailureOntoMainFailure() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final IllegalStateException subOwn = new IllegalStateException("sub own");
            final CountDownLatch subscriberFailed = new CountDownLatch(1);

            final Stream<Integer> main = attach(op, upstreamFailingAfter(subscriberFailed, boom), s -> {
                try {
                    throw subOwn;
                } finally {
                    subscriberFailed.countDown();
                }
            });

            final Throwable thrown = assertThrows(IllegalArgumentException.class, main::toList);
            assertSame(boom, thrown, "op " + op);
            assertEquals(1, boom.getSuppressed().length, "op " + op);
            assertSame(subOwn, boom.getSuppressed()[0], "op " + op); // the same instance, not a wrapper
        }
    }

    @Test
    public void testC130_mainFailureHandledByCaller_derivedSubscriberFailureIsDropped() {
        for (int op = 0; op < 4; op++) {
            final IllegalArgumentException boom = new IllegalArgumentException("up boom");
            final Stream<Integer> main = attach(op, Stream.of(1, 2, 3, 4).map(x -> {
                if (x == 3) {
                    throw boom;
                }

                return x;
            }), s -> s.forEach(x -> {
            })); // rethrows the "main stream failed" signal

            final Iterator<Integer> it = main.iterator();

            assertThrows(IllegalArgumentException.class, () -> {
                while (it.hasNext()) {
                    it.next();
                }
            });

            main.close(); // nothing independent to report
            assertEquals(0, boom.getSuppressed().length, "op " + op);
        }
    }

    // ============================================================== C-117: no upstream pull by the subscriber after a main failure

    private static Stream<Integer> attachAllPass(final int op, final Stream<Integer> upstream,
            final Throwables.Consumer<? super Stream<Integer>, ? extends Exception> subscriber) {
        return switch (op) {
            case 0 -> upstream.addSubscriber(subscriber);
            case 1 -> upstream.filterWhileAddSubscriber(x -> x > 0, subscriber);
            case 2 -> upstream.takeWhileAddSubscriber(x -> x > 0, subscriber);
            default -> upstream.dropWhileAddSubscriber(x -> x > 0, subscriber);
        };
    }

    /** 10,000 elements (never an unbounded source: an unfixed build must fail an assertion, not hang). */
    private static Stream<Integer> countedSourceFailingAt(final int failAt, final AtomicInteger pulls, final RuntimeException boom) {
        return Stream.range(1, 10_001).peek(x -> pulls.incrementAndGet()).map(x -> {
            if (x == failAt) {
                throw boom;
            }

            return x;
        });
    }

    @Test
    public void testC117_subscriberDoesNotPullUpstreamAfterMainFailure_allFourVariants() {
        for (final int failAt : new int[] { 1, 3 }) {
            for (int op = 0; op < 4; op++) {
                final IllegalArgumentException boom = new IllegalArgumentException("boom");
                final AtomicInteger pulls = new AtomicInteger();
                final AtomicReference<Throwable> seen = new AtomicReference<>();

                final Stream<Integer> main = attachAllPass(op, countedSourceFailingAt(failAt, pulls, boom), s -> {
                    try {
                        s.forEach(x -> {
                        });
                    } catch (final IllegalStateException e) {
                        seen.set(e);
                        throw e;
                    }
                });

                final Throwable thrown = assertThrows(IllegalArgumentException.class, main::toList);
                final String ctx = "op " + op + ", failAt " + failAt;

                assertSame(boom, thrown, ctx);
                assertEquals(failAt, pulls.get(), ctx + ": upstream pulls after the failure");
                assertEquals(0, boom.getSuppressed().length, ctx);
                assertTrue(seen.get() instanceof IllegalStateException, ctx + ": " + seen.get());
                assertSame(boom, seen.get().getCause(), ctx);
            }
        }
    }

    @Test
    public void testC117_subscriberCatchesAndRetries_stillNoUpstreamPull() {
        for (final int failAt : new int[] { 1, 3 }) {
            for (int op = 0; op < 4; op++) {
                final IllegalArgumentException boom = new IllegalArgumentException("boom");
                final AtomicInteger pulls = new AtomicInteger();
                final List<Throwable> seen = new CopyOnWriteArrayList<>();

                final Stream<Integer> main = attachAllPass(op, countedSourceFailingAt(failAt, pulls, boom), s -> {
                    final Iterator<Integer> it = s.iterator();

                    for (int attempt = 0; attempt < 3; attempt++) {
                        try {
                            while (it.hasNext()) {
                                it.next();
                            }
                        } catch (final IllegalStateException e) {
                            seen.add(e);
                        }
                    }
                });

                final String ctx = "op " + op + ", failAt " + failAt;

                assertSame(boom, assertThrows(IllegalArgumentException.class, main::toList), ctx);
                assertEquals(failAt, pulls.get(), ctx + ": upstream pulls after the failure");
                assertEquals(3, seen.size(), ctx + ": " + seen);

                for (final Throwable e : seen) {
                    assertSame(boom, e.getCause(), ctx);
                }
            }
        }
    }

    /** The success paths (including the documented drain after the main stream stops early) are unchanged. */
    @Test
    public void testC117_successPathsUnchanged() {
        final List<Integer> ten = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        final List<Integer> sub1 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(1, 2, 4, 5, 7, 8, 10), Stream.of(ten.iterator()).filterWhileAddSubscriber(x -> x % 3 != 0, s -> s.forEach(sub1::add)).toList());
        assertEquals(List.of(3, 6, 9), sub1);

        final List<Integer> sub2 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(1, 2, 3), Stream.of(ten.iterator()).takeWhileAddSubscriber(x -> x < 4, s -> s.forEach(sub2::add)).toList());
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10), sub2);

        final List<Integer> sub3 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10), Stream.of(ten.iterator()).dropWhileAddSubscriber(x -> x < 4, s -> s.forEach(sub3::add)).toList());
        assertEquals(List.of(1, 2, 3), sub3);

        // The main stream stops early (limit): the subscriber drains the rest of the upstream itself, as documented.
        final List<Integer> sub4 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(1, 2), Stream.of(ten.iterator()).filterWhileAddSubscriber(x -> x % 3 != 0, s -> s.forEach(sub4::add)).limit(2).toList());
        assertEquals(List.of(3, 6, 9), sub4);

        final List<Integer> sub5 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(1, 2), Stream.of(ten.iterator()).takeWhileAddSubscriber(x -> x < 4, s -> s.forEach(sub5::add)).limit(2).toList());
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10), sub5);

        final List<Integer> sub6 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(), Stream.of(ten.iterator()).dropWhileAddSubscriber(x -> x < 4, s -> s.forEach(sub6::add)).limit(0).toList());
        assertEquals(List.of(1, 2, 3), sub6);

        final List<Integer> sub7 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(1, 2), Stream.of(ten.iterator()).addSubscriber(s -> s.forEach(sub7::add)).limit(2).toList());
        assertEquals(ten, sub7);

        // empty upstream
        final List<Integer> sub8 = new CopyOnWriteArrayList<>();
        assertEquals(List.of(), Stream.<Integer> empty().filterWhileAddSubscriber(x -> x > 0, s -> s.forEach(sub8::add)).toList());
        assertEquals(List.of(), sub8);
    }

    // ============================================================== C-131: interval waits uninterruptibly; the documented idiom stops it

    @Test
    public void testC131_interval_takeWhileNotInterruptedIdiomStopsConsumer() throws Exception {
        final AtomicInteger consumed = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread t = new Thread(() -> {
            try {
                // limit(500) only bounds the test if the idiom were broken (500 x 10 ms).
                Stream.interval(10, () -> 1).takeWhile(x -> !Thread.currentThread().isInterrupted()).limit(500).forEach(x -> consumed.incrementAndGet());
            } catch (final Throwable e) {
                failure.set(e);
            }
        });

        t.setDaemon(true);
        t.start();

        final long deadline = System.currentTimeMillis() + 10_000;

        while (consumed.get() < 3 && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }

        t.interrupt();
        t.join(2_000);

        assertFalse(t.isAlive());
        assertEquals(null, failure.get()); // the interrupt ended the traversal through takeWhile, not through an exception
        assertTrue(consumed.get() < 500, "consumed " + consumed.get());
    }

    // ============================================================== G2-06: takeWhileAddSubscriber huge queue; interrupt paths

    /**
     * takeWhileAddSubscriber has no queue-size overload (its queue always has the default size), so the C-090 huge-queue
     * case cannot arise for it; this locks the executor overload the family test does not cover.
     */
    @Test
    public void testG206_takeWhileAddSubscriber_executorOverload() {
        final ExecutorService executor = Executors.newCachedThreadPool();

        try {
            final AtomicInteger closed = new AtomicInteger();
            final List<Integer> sub = new CopyOnWriteArrayList<>();
            final List<Integer> result = Stream.of(List.of(1, 2, 3, 4).iterator())
                    .onClose(closed::incrementAndGet)
                    .takeWhileAddSubscriber(x -> x < 3, s -> s.forEach(sub::add), executor)
                    .toList();

            assertEquals(List.of(1, 2), result);
            assertEquals(List.of(3, 4), sub);
            assertEquals(1, closed.get());
        } finally {
            executor.shutdownNow();
        }
    }

    /** Runs {@code body} on a new thread, interrupts it once it is waiting for the subscriber, and returns what it threw. */
    private static Throwable interruptWhileAwaitingSubscriber(final java.util.function.Supplier<Stream<Integer>> mainFactory,
            final AtomicBoolean mainDone, final AtomicBoolean interruptFlagAfter) throws Exception {
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final Thread t = new Thread(() -> {
            try {
                mainFactory.get().toList();
            } catch (final Throwable e) {
                thrown.set(e);
            } finally {
                interruptFlagAfter.set(Thread.currentThread().isInterrupted());
            }
        });

        t.setDaemon(true);
        t.start();

        final long deadline = System.currentTimeMillis() + 10_000;

        while (!(mainDone.get() && t.getState() == Thread.State.WAITING) && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }

        t.interrupt();
        t.join(10_000);
        assertFalse(t.isAlive());

        return thrown.get();
    }

    @Test
    public void testG206_interruptWhileAwaitingSubscriber_noMainFailure() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean mainDone = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();

        try {
            final Throwable thrown = interruptWhileAwaitingSubscriber(() -> Stream.of(1, 2, 3).addSubscriber(s -> {
                release.await(10, TimeUnit.SECONDS);
                s.count();
            }).peek(x -> {
                if (x == 3) {
                    mainDone.set(true);
                }
            }), mainDone, interrupted);

            assertTrue(thrown instanceof UncheckedInterruptedException, String.valueOf(thrown));
            assertTrue(interrupted.get(), "interrupt status kept");
        } finally {
            release.countDown();
        }
    }

    @Test
    public void testG206_interruptWhileAwaitingSubscriber_withMainFailure() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean mainDone = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final IllegalArgumentException boom = new IllegalArgumentException("boom");

        try {
            final Throwable thrown = interruptWhileAwaitingSubscriber(() -> Stream.of(1, 2, 3).map(x -> {
                if (x == 3) {
                    mainDone.set(true);
                    throw boom;
                }

                return x;
            }).addSubscriber(s -> {
                release.await(10, TimeUnit.SECONDS);
                s.count();
            }), mainDone, interrupted);

            assertSame(boom, thrown);
            assertEquals(1, boom.getSuppressed().length);
            assertTrue(boom.getSuppressed()[0] instanceof UncheckedInterruptedException, String.valueOf(boom.getSuppressed()[0]));
            assertTrue(interrupted.get(), "interrupt status kept");
        } finally {
            release.countDown();
        }
    }

    // ============================================================== G2-05: merge tree vs left fold for consistent tie policies

    @Test
    public void testG205_mergeTree_equalsLeftFold_forEitherConsistentTiePolicy() {
        final BiFunction<int[], int[], MergeResult> firstOnTies = (a, b) -> a[0] <= b[0] ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        final BiFunction<int[], int[], MergeResult> secondOnTies = (a, b) -> a[0] < b[0] ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        for (final BiFunction<int[], int[], MergeResult> selector : List.of(firstOnTies, secondOnTies)) {
            for (int k = 3; k <= 9; k++) {
                final List<List<int[]>> sources = new ArrayList<>();

                for (int i = 0; i < k; i++) {
                    sources.add(List.of(new int[] { 1, i }, new int[] { 2, i }, new int[] { 2 + i % 2, i }));
                }

                Stream<int[]> fold = Stream.of(sources.get(0));

                for (int i = 1; i < k; i++) {
                    fold = Stream.merge(fold, Stream.of(sources.get(i)), selector);
                }

                final List<String> expected = fold.map(Arrays::toString).toList();
                final List<String> tree = Stream.merge(Stream.of(sources).map(l -> Stream.of(l)).toList(), selector).map(Arrays::toString).toList();
                final List<String> treeIterators = Stream.mergeIterators(Stream.of(sources).map(List::iterator).toList(), selector)
                        .map(Arrays::toString)
                        .toList();

                assertEquals(expected, tree, "k=" + k);
                assertEquals(expected, treeIterators, "k=" + k);
            }
        }
    }

    // ============================================================== H1-02: concat closes exhausted inputs early (documented)

    @Test
    public void testH102_concat_closesExhaustedInputsEarly_closeFailureEndsTraversal() {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Integer> result = Stream.concat(Stream.of(1).onClose(() -> events.add("a closed")), Stream.of(2).onClose(() -> events.add("b closed")))
                .peek(x -> events.add("read " + x))
                .toList();

        assertEquals(List.of(1, 2), result);
        assertEquals(List.of("read 1", "a closed", "read 2", "b closed"), events);

        final IllegalStateException closeA = new IllegalStateException("A");
        final AtomicInteger closedC = new AtomicInteger();
        final List<Integer> delivered = new ArrayList<>();
        final Throwable thrown = assertThrows(IllegalStateException.class,
                () -> Stream.concat(List.of(Stream.of(1).onClose(() -> {
                    throw closeA;
                }), Stream.of(2), Stream.of(3).onClose(closedC::incrementAndGet))).forEach(delivered::add));

        assertSame(closeA, thrown);
        assertEquals(List.of(1), delivered);
        assertEquals(1, closedC.get()); // the remaining inputs are still closed with the resulting stream
    }
}
