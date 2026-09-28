package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

/**
 * Pins the fixes of the 2026-09-23 ContinuableFuture / Futures review
 * ({@code scripts/cross_review/ContinuableFuture_Futures_ledger_2026-09-23.md}, C-001..C-035). Tests named after a
 * finding pin its fix and are red on the classes that preceded it; the ones marked "pins unchanged behaviour" guard a
 * contract the fixes deliberately kept.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public class ContinuableFutureFuturesReview20260923Test extends TestBase {

    private static final Executor DIRECT = Runnable::run;

    private static <T> ContinuableFuture<T> failed(final Exception failure) {
        final ContinuableFuture<T> future = ContinuableFuture.call(() -> {
            throw failure;
        });
        future.getAsResult();
        return future;
    }

    private static <T> ContinuableFuture<T> failedWithError(final Error failure) {
        final ContinuableFuture<T> future = ContinuableFuture.call(() -> {
            throw failure;
        });
        future.getAsResult();
        return future;
    }

    private static <T> ContinuableFuture<T> cancelledTask() {
        final FutureTask<T> task = new FutureTask<>(() -> null);
        task.cancel(false);
        return ContinuableFuture.wrap(task);
    }

    // ---------------------------------------------------------------- C-001: one wrapper per failure, every family

    @Test
    public void c001_strictThenOverloadsWrapTheUpstreamFailureOnce() throws Exception {
        final IllegalStateException boom = new IllegalStateException("boom");
        final ContinuableFuture<String> upstream = failed(boom);
        final AtomicBoolean ran = new AtomicBoolean();

        final List<ContinuableFuture<?>> stages = List.of(upstream.thenRunAsync(() -> ran.set(true)), upstream.thenRunAsync(s -> ran.set(true)),
                upstream.thenCallAsync(() -> {
                    ran.set(true);
                    return "x";
                }), upstream.thenCallAsync(s -> {
                    ran.set(true);
                    return s;
                }));

        for (final ContinuableFuture<?> stage : stages) {
            final ExecutionException ex = assertThrows(ExecutionException.class, stage::get);
            assertSame(boom, ex.getCause(), "the cause must be the upstream's own failure, not its ExecutionException");
            assertFalse(stage.isCancelled());
        }

        assertFalse(ran.get(), "a strict continuation must not run after an upstream failure");
    }

    @Test
    public void c001_longChainStaysSingleWrapped() {
        final IllegalStateException boom = new IllegalStateException("boom");
        final ContinuableFuture<String> chain = ContinuableFutureFuturesReview20260923Test.<String> failed(boom)
                .thenCallAsync(s -> s + 1)
                .thenCallAsync(s -> s + 2)
                .thenRunAsync(s -> {
                })
                .thenCallAsync(() -> "end");

        final ExecutionException ex = assertThrows(ExecutionException.class, chain::get);
        assertSame(boom, ex.getCause());
    }

    @Test
    public void c001_errorFailuresAreReportedAsTheErrorInEveryFamily() throws Exception {
        final AssertionError err = new AssertionError("err");
        final AssertionError err2 = new AssertionError("err2");
        final ContinuableFuture<String> bad = failedWithError(err);
        final ContinuableFuture<String> bad2 = failedWithError(err2);
        final ContinuableFuture<String> ok = ContinuableFuture.completed("ok");

        assertSame(err, assertThrows(ExecutionException.class, () -> bad.thenCallAsync(s -> s).get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> bad.thenCallAsync(() -> "x").get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> bad.thenRunAsync(() -> {
        }).get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> bad.thenRunAsync(s -> {
        }).get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> bad.callAsyncAfterBoth(ok, (a, b) -> a).get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> ok.runAsyncAfterBoth(bad, () -> {
        }).get()).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> bad.map(s -> s).get()).getCause());

        final ExecutionException bothFailed = assertThrows(ExecutionException.class, () -> bad.callAsyncAfterFirstSuccess(bad2, s -> s).get());
        assertSame(err, bothFailed.getCause());
        assertArrayEquals(new Throwable[] { err2 }, err.getSuppressed(), "the second Error is attached as itself, once");

        // The exception slot of the lenient callbacks is typed Exception, so an Error still arrives in its carrier.
        final AtomicReference<Exception> seen = new AtomicReference<>();
        bad.thenRunAsync((value, exception) -> seen.set(exception)).get();
        assertInstanceOf(ExecutionException.class, seen.get());
        assertSame(err, seen.get().getCause());
    }

    @Test
    public void c001_cancelledUpstreamShapeIsUnchanged() {
        final ContinuableFuture<String> cancelled = cancelledTask();
        final ContinuableFuture<String> child = cancelled.thenCallAsync(s -> s);

        final ExecutionException ex = assertThrows(ExecutionException.class, child::get);
        assertInstanceOf(CancellationException.class, ex.getCause());
        assertFalse(child.isCancelled());
    }

    // ---------------------------------------------------------------- C-002: wrap(ContinuableFuture) is the identity

    @Test
    public void c002_wrapReturnsAContinuableFutureUnchanged() throws Exception {
        final ContinuableFuture<String> plain = ContinuableFuture.completed("v");
        assertSame(plain, ContinuableFuture.wrap(plain));

        final ContinuableFuture<String> mapped = plain.map(s -> s);
        assertSame(mapped, ContinuableFuture.wrap(mapped));

        final FutureTask<String> task = new FutureTask<>(() -> "t");
        task.run();
        assertNotSame(task, ContinuableFuture.wrap(task));
        assertEquals("t", ContinuableFuture.wrap(task).get());
    }

    @Test
    public void c002_wrapKeepsTheChainAndTheExecutor() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ContinuableFuture<String> root = ContinuableFuture.call(() -> {
            release.await();
            return "root";
        });
        final ExecutorService custom = Executors.newSingleThreadExecutor(r -> new Thread(r, "c002-custom"));
        final AtomicReference<String> ranOn = new AtomicReference<>();

        try {
            final ContinuableFuture<String> mid = root.thenUse(custom).thenCallAsync(s -> s + "-mid");
            final ContinuableFuture<String> viaWrap = ContinuableFuture.wrap(mid).thenCallAsync(s -> {
                ranOn.set(Thread.currentThread().getName());
                return s;
            });

            assertTrue(ContinuableFuture.wrap(mid).cancelAll(true));
            assertTrue(root.isCancelled(), "cancelAll through wrap() must reach the root");
            assertTrue(ContinuableFuture.wrap(mid).isAllCancelled());
            assertThrows(ExecutionException.class, viaWrap::get);
        } finally {
            release.countDown();
            custom.shutdownNow();
        }

        final ContinuableFuture<String> done = ContinuableFuture.completed("d");
        final ExecutorService custom2 = Executors.newSingleThreadExecutor(r -> new Thread(r, "c002-custom2"));
        try {
            final String thread = ContinuableFuture.wrap(done.thenUse(custom2)).thenCallAsync(() -> Thread.currentThread().getName()).get();
            assertEquals("c002-custom2", thread, "the next stage must keep the executor chosen with thenUse");
        } finally {
            custom2.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- C-003: the delay window is a cancellable stage

    @Test
    public void c003_cancelDuringTheDelayWindowEndsTheStageAndWakesGetters() throws Exception {
        final ContinuableFuture<String> original = ContinuableFuture.completed("v");
        final ContinuableFuture<String> delayed = original.thenDelay(10, TimeUnit.SECONDS);
        final CountDownLatch started = new CountDownLatch(2);
        final AtomicReference<Throwable> untimed = new AtomicReference<>();
        final AtomicReference<Throwable> timed = new AtomicReference<>();
        final Thread untimedGetter = new Thread(() -> {
            started.countDown();
            try {
                delayed.get();
            } catch (final Throwable t) {
                untimed.set(t);
            }
        });
        final Thread timedGetter = new Thread(() -> {
            started.countDown();
            try {
                delayed.get(9, TimeUnit.SECONDS);
            } catch (final Throwable t) {
                timed.set(t);
            }
        });
        untimedGetter.start();
        timedGetter.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertFalse(delayed.isDone());

        final long before = System.nanoTime();
        assertTrue(delayed.cancel(true));
        untimedGetter.join(5000);
        timedGetter.join(5000);
        assertFalse(untimedGetter.isAlive());
        assertFalse(timedGetter.isAlive());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before) < 4000, "a parked getter must wake up at once");

        assertInstanceOf(CancellationException.class, untimed.get());
        assertInstanceOf(CancellationException.class, timed.get());
        assertTrue(delayed.isCancelled());
        assertTrue(delayed.isDone());
        assertThrows(CancellationException.class, delayed::get);
        assertThrows(CancellationException.class, () -> delayed.get(1, TimeUnit.SECONDS));
        assertFalse(delayed.cancel(true), "a second cancel() finds the stage already cancelled");
        assertEquals("v", original.get());
        assertFalse(original.isCancelled());
    }

    @Test
    public void c003_cancelAfterTheDelayElapsedIsRefused() throws Exception {
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("v").thenDelay(30, TimeUnit.MILLISECONDS);
        assertEquals("v", delayed.get());
        assertTrue(delayed.isDone());
        assertFalse(delayed.cancel(true));
        assertFalse(delayed.isCancelled());
        assertEquals("v", delayed.get());
    }

    @Test
    public void c003_thenUseOverACompletedUpstreamIsCompleteAtOnce() throws Exception {
        final ContinuableFuture<String> switched = ContinuableFuture.completed("v").thenUse(DIRECT);
        assertTrue(switched.isDone());
        assertFalse(switched.cancel(true));
        assertFalse(switched.isCancelled());
        assertEquals("v", switched.get());
    }

    @Test
    public void c003_downstreamStageOfACancelledWindowFailsWithCancellation() throws Exception {
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("v").thenDelay(10, TimeUnit.SECONDS);
        final ContinuableFuture<String> downstream = delayed.thenCallAsync(s -> s + "!");
        Thread.sleep(100);

        assertTrue(delayed.cancel(true));

        final ExecutionException ex = assertThrows(ExecutionException.class, () -> downstream.get(5, TimeUnit.SECONDS));
        assertInstanceOf(CancellationException.class, ex.getCause());
    }

    @Test
    public void c003_cancelAllOverACompletedUpstreamEndsTheWindowButReportsFalse() throws Exception {
        final ContinuableFuture<String> original = ContinuableFuture.completed("v");
        final ContinuableFuture<String> delayed = original.thenDelay(10, TimeUnit.SECONDS).thenUse(DIRECT);

        assertFalse(delayed.cancelAll(true), "a completed upstream stage was not cancelled");
        assertTrue(delayed.isCancelled());
        assertTrue(delayed.isDone());
        assertFalse(delayed.isAllCancelled());
        assertThrows(CancellationException.class, delayed::get);
        assertEquals("v", original.get());
    }

    @Test
    public void c003_pendingUpstreamCancellationStillBypassesTheDelay() {
        final FutureTask<String> pending = new FutureTask<>(() -> "never");
        final ContinuableFuture<String> delayed = ContinuableFuture.wrap(pending).thenDelay(1, TimeUnit.DAYS);

        assertTrue(delayed.cancel(false));
        assertTrue(pending.isCancelled());
        assertTrue(delayed.isCancelled());
        assertTrue(delayed.isDone());
        assertThrows(CancellationException.class, delayed::get);
        assertFalse(delayed.cancel(false));
    }

    @Test
    public void c003_cancelInTheWindowOverAFailedUpstreamReportsCancellation() {
        final ContinuableFuture<String> delayed = ContinuableFutureFuturesReview20260923Test.<String> failed(new IllegalStateException("boom"))
                .thenDelay(10, TimeUnit.SECONDS);

        assertFalse(delayed.isDone());
        assertTrue(delayed.cancel(false));
        assertTrue(delayed.isCancelled());
        assertTrue(delayed.isDone());
        assertThrows(CancellationException.class, delayed::get);
        assertThrows(CancellationException.class, () -> delayed.get(1, TimeUnit.SECONDS));
        assertFalse(delayed.cancel(true), "already cancelled");
    }

    @Test
    public void c003_aGetterInterruptedInTheWindowLeavesTheStagePending() throws Exception {
        // Pins unchanged behaviour: an interrupted getter never ended the window, before or after C-003.
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("v").thenDelay(400, TimeUnit.MILLISECONDS);
        final AtomicReference<Throwable> seen = new AtomicReference<>();
        final Thread getter = new Thread(() -> {
            try {
                delayed.get();
            } catch (final Throwable t) {
                seen.set(t);
            }
        });
        getter.start();
        Thread.sleep(50);
        getter.interrupt();
        getter.join(5000);

        assertInstanceOf(InterruptedException.class, seen.get());
        assertFalse(delayed.isCancelled(), "an interrupted getter does not end the window");
        assertFalse(delayed.isDone(), "the window is still open");
        assertEquals("v", delayed.get(), "the window elapses normally for everyone else");
    }

    @Test
    public void c003_cancelAndElapseAreMutuallyExclusive() throws Exception {
        final ExecutorService getters = Executors.newSingleThreadExecutor();
        try {
            for (int round = 0; round < 200; round++) {
                final ContinuableFuture<String> delayed = ContinuableFuture.completed("v").thenDelay(1, TimeUnit.MILLISECONDS);
                final Future<Object> outcome = getters.submit(() -> {
                    try {
                        return delayed.get();
                    } catch (final CancellationException e) {
                        return e;
                    }
                });
                if (ThreadLocalRandom.current().nextBoolean()) {
                    Thread.yield();
                }
                final boolean cancelled = delayed.cancel(true);
                final Object seen = outcome.get(5, TimeUnit.SECONDS);

                if (cancelled) {
                    assertInstanceOf(CancellationException.class, seen, "round " + round + ": cancel() succeeded, so no getter may have returned the value");
                    assertTrue(delayed.isCancelled());
                } else {
                    assertEquals("v", seen, "round " + round + ": cancel() was refused, so the value was delivered");
                    assertFalse(delayed.isCancelled());
                }
                assertTrue(delayed.isDone());
            }
        } finally {
            getters.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- C-004: single-zip compose honours the timed budget

    @Test
    public void c004_singleZipComposeTimedGetHonoursItsBudgetWithAPendingInput() throws Exception {
        final CompletableFuture<Integer> pending = new CompletableFuture<>();
        final CompletableFuture<Integer> done = CompletableFuture.completedFuture(1);
        try {
            final ContinuableFuture<Integer> two = Futures.compose(pending, done, (a, b) -> a.get() + b.get());
            final ContinuableFuture<Integer> three = Futures.compose(pending, done, done, (a, b, c) -> a.get() + b.get() + c.get());
            final ContinuableFuture<Integer> many = Futures.compose(List.of(pending, done), futures -> {
                int sum = 0;
                for (final Future<? extends Integer> f : futures) {
                    sum += f.get();
                }
                return sum;
            });

            for (final ContinuableFuture<Integer> composed : List.of(two, three, many)) {
                final long start = System.nanoTime();
                assertThrows(TimeoutException.class, () -> composed.get(100, TimeUnit.MILLISECONDS));
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 3000, "the timed get must return around its budget");
                assertFalse(composed.isDone());
            }

            pending.complete(2);
            assertEquals(3, two.get(1, TimeUnit.SECONDS));
            assertEquals(4, three.get(1, TimeUnit.SECONDS));
            assertEquals(3, many.get(1, TimeUnit.SECONDS));
            assertEquals(3, two.get());
        } finally {
            pending.complete(2);
        }
    }

    @Test
    public void c004_singleZipComposeShortCircuitBehavesTheSameUnderTimedAndUntimedGet() throws Exception {
        final CompletableFuture<Boolean> condition = CompletableFuture.completedFuture(false);
        final CompletableFuture<String> expensive = new CompletableFuture<>();
        try {
            final ContinuableFuture<String> composed = Futures.compose(condition, expensive, (c, d) -> c.get() ? d.get() : "DEFAULT");

            assertEquals("DEFAULT", composed.get(100, TimeUnit.MILLISECONDS), "a zip that skips the pending input never waits for it");
            assertEquals("DEFAULT", composed.get());
        } finally {
            expensive.complete("unused");
        }
    }

    @Test
    public void c004_boundedInputViewsDelegateStateAndCapTheirOwnTimeouts() throws Exception {
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");
        final List<String> observations = Collections.synchronizedList(new ArrayList<>());
        try {
            final ContinuableFuture<String> composed = Futures.compose(pending, done, (p, d) -> {
                observations.add("pending.isDone=" + p.isDone() + " done.isDone=" + d.isDone());
                try {
                    p.get(5, TimeUnit.SECONDS); // capped by the 100 ms budget of the timed get below
                    return "unexpected";
                } catch (final TimeoutException e) {
                    observations.add("capped");
                    return d.get();
                }
            });

            final long start = System.nanoTime();
            assertEquals("d", composed.get(100, TimeUnit.MILLISECONDS));
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000, "the view's own 5 s timeout must be capped by the 100 ms budget");
            assertEquals(List.of("pending.isDone=false done.isDone=true", "capped"), observations);
        } finally {
            pending.complete("p");
        }
    }

    @Test
    public void c004_nonPositiveTimedGetPollsAndViewsDelegateOutcomesAndCancellation() throws Exception {
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");
        try {
            final ContinuableFuture<String> composed = Futures.compose(pending, done, (p, d) -> p.get() + d.get());
            assertThrows(TimeoutException.class, () -> composed.get(0, TimeUnit.NANOSECONDS));
            assertThrows(TimeoutException.class, () -> composed.get(-5, TimeUnit.SECONDS));

            final IllegalStateException boom = new IllegalStateException("boom");
            final FutureTask<String> failedTask = new FutureTask<>(() -> {
                throw boom;
            });
            failedTask.run();
            final FutureTask<String> cancelledTask = new FutureTask<>(() -> "never");
            cancelledTask.cancel(false);
            final List<String> seen = new ArrayList<>();
            final ContinuableFuture<String> outcomes = Futures.compose(failedTask, cancelledTask, (f, c) -> {
                try {
                    f.get();
                } catch (final ExecutionException e) {
                    seen.add("failed:" + (e.getCause() == boom));
                }
                try {
                    c.get();
                } catch (final CancellationException e) {
                    seen.add("cancelled:" + c.isCancelled());
                }
                return "seen";
            });
            assertEquals("seen", outcomes.get(1, TimeUnit.SECONDS));
            assertEquals(List.of("failed:true", "cancelled:true"), seen, "failures and cancellation reach the zip through the views unchanged");

            final CompletableFuture<String> toCancel = new CompletableFuture<>();
            final List<Boolean> viewState = new ArrayList<>();
            final ContinuableFuture<String> cancelling = Futures.compose(toCancel, done, (t, d) -> {
                viewState.add(t.isDone());
                viewState.add(t.cancel(true));
                return d.get();
            });
            assertEquals("d", cancelling.get(1, TimeUnit.SECONDS));
            assertEquals(List.of(false, true), viewState, "the view reports the input's state and cancel() delegates to the input");
            assertTrue(toCancel.isCancelled());
        } finally {
            pending.complete("p");
        }
    }

    @Test
    public void c004_composeTimedGetRejectsANullUnit() {
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");
        final ContinuableFuture<String> singleZip = Futures.compose(done, done, (a, b) -> a.get() + b.get());
        final ContinuableFuture<String> twoZip = Futures.compose(done, done, (a, b) -> a.get() + b.get(), t -> "timed");

        assertThrows(NullPointerException.class, () -> singleZip.get(1, null));
        assertThrows(NullPointerException.class, () -> twoZip.get(1, null));
    }

    // ---------------------------------------------------------------- C-005: iterate no longer stalls on a done lazy input

    @Test
    public void c005_iterateRelaysADoneLazilyMappedInputInsteadOfStallingTheFactory() throws Exception {
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("lazy").map(v -> {
            Thread.sleep(3000); // interrupted by the relay release at the end, so it costs no wall time
            return v;
        });
        assertTrue(lazy.isDone());
        final CompletableFuture<String> sibling = CompletableFuture.completedFuture("sibling");

        final long start = System.nanoTime();
        final ObjIterator<Result<String, Exception>> iter = Futures.iterate(List.of(lazy, sibling), 200, TimeUnit.MILLISECONDS, Function.identity());
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000, "the factory must not run the mapper inline");

        assertEquals("sibling", iter.next().orElseThrow());
        assertInstanceOf(TimeoutException.class, iter.next().getException(), "the lazy input is late for the 200 ms budget");
        assertFalse(iter.hasNext());
    }

    @Test
    public void c005_iterateYieldsCompletedPlainTasksInInputOrder() throws Exception {
        final ContinuableFuture<String> called = ContinuableFuture.call(() -> "c");
        called.get();
        final FutureTask<String> task = new FutureTask<>(() -> "d");
        task.run();

        final ObjIterator<String> iter = Futures.iterate(List.of(ContinuableFuture.completed("a"), ContinuableFuture.completed("b"), called, task));

        assertEquals(List.of("a", "b", "c", "d"), iter.toList());
    }

    @Test
    public void c005_anyOfRelaysADoneLazilyMappedInputInsteadOfRunningItsMapperInline() throws Exception {
        // Pins unchanged behaviour: anyOf never read a lazily mapped input inline; the rule is now shared with iterate.
        final ContinuableFuture<String> lazy = ContinuableFuture.completed("lazy").map(v -> {
            Thread.sleep(3000); // interrupted when the last waiter leaves, so it costs no wall time
            return v;
        });
        final CompletableFuture<String> sibling = CompletableFuture.completedFuture("sibling");

        final long start = System.nanoTime();
        assertEquals("sibling", Futures.anyOf(lazy, sibling).get(300, TimeUnit.MILLISECONDS));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000, "the mapper must not run on the waiting caller");
    }

    @Test
    public void c005_hasInstantOutcomeRecognisesOnlyCompletedPlainTasks() throws Exception {
        // Structural pin of the package-private classification rule shared by iterate/anyOf: which done inputs are
        // read inline (no relay thread) and which are relayed because their get() may run user code or wait.
        final FutureTask<String> plain = new FutureTask<>(() -> "p");
        assertFalse(Futures.hasInstantOutcome(plain), "not done yet");
        plain.run();
        assertTrue(Futures.hasInstantOutcome(plain));

        final FutureTask<String> subclass = new FutureTask<>(() -> "s") {
        };
        subclass.run();
        assertFalse(Futures.hasInstantOutcome(subclass), "a subclass may override get()");

        final ContinuableFuture<String> called = ContinuableFuture.call(() -> "c");
        called.get();
        assertTrue(Futures.hasInstantOutcome(called));
        assertTrue(Futures.hasInstantOutcome(ContinuableFuture.completed("x")));
        assertTrue(Futures.hasInstantOutcome(ContinuableFuture.wrap(CompletableFuture.completedFuture("x"))), "a done exact CompletableFuture only reports");
        assertFalse(Futures.hasInstantOutcome(ContinuableFuture.wrap(new CompletableFuture<String>())), "not done");
        assertFalse(Futures.hasInstantOutcome(ContinuableFuture.completed("x").map(v -> v)));
        assertFalse(Futures.hasInstantOutcome(ContinuableFuture.completed("x").thenDelay(1, TimeUnit.DAYS)));
        assertFalse(Futures.hasInstantOutcome(ContinuableFuture.completed("x").thenUse(DIRECT)));
        assertFalse(Futures.hasInstantOutcome(CompletableFuture.completedFuture("x")));
        assertFalse(Futures.hasInstantOutcome(ContinuableFuture.wrap(subclass)));
    }

    // ---------------------------------------------------------------- C-006: allOf isCancelled() implies CancellationException

    @Test
    public void c006_allOfReportsCancellationWhenCancelledEvenIfAFailurePrecedes() throws Exception {
        final IllegalStateException boom = new IllegalStateException("boom");
        final FutureTask<String> failed = new FutureTask<>(() -> {
            throw boom;
        });
        failed.run();
        final FutureTask<String> cancelled = new FutureTask<>(() -> "never");
        cancelled.cancel(false);

        final ContinuableFuture<List<String>> failedFirst = Futures.allOf(failed, cancelled);
        assertTrue(failedFirst.isDone());
        assertTrue(failedFirst.isCancelled());
        final CancellationException ce = assertThrows(CancellationException.class, failedFirst::get);
        assertArrayEquals(new Throwable[] { boom }, ce.getSuppressed(), "the failure met first stays visible");
        assertThrows(CancellationException.class, () -> failedFirst.get(1, TimeUnit.SECONDS));

        final ContinuableFuture<List<String>> cancelledFirst = Futures.allOf(cancelled, failed);
        assertTrue(cancelledFirst.isCancelled());
        assertThrows(CancellationException.class, cancelledFirst::get);

        assertThrows(CancellationException.class, () -> Futures.combine(failed, cancelled).get());
        assertThrows(CancellationException.class, () -> Futures.combine(failed, cancelled, (a, b) -> a + b).get());

        // Not cancelled while a sibling is still pending: the failure is reported as such.
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<List<String>> stillPending = Futures.allOf(failed, pending);
        assertFalse(stillPending.isCancelled());
        assertSame(boom, assertThrows(ExecutionException.class, stillPending::get).getCause());
        pending.complete("late");
    }

    // ---------------------------------------------------------------- C-007: suppressOnce is atomic, bounded and acyclic

    @Test
    public void c007_concurrentCombinationsOfOneFailedPairAttachTheSecondFailureOnce() throws Exception {
        final IllegalStateException exA = new IllegalStateException("a");
        final IllegalStateException exB = new IllegalStateException("b");
        final ContinuableFuture<String> a = failed(exA);
        final ContinuableFuture<String> b = failed(exB);
        final ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            for (int round = 0; round < 40; round++) {
                final CountDownLatch go = new CountDownLatch(1);
                final List<Future<?>> tasks = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    tasks.add(pool.submit(() -> {
                        go.await();
                        a.thenUse(DIRECT).callAsyncAfterBoth(b, (x, y) -> x).getAsResult();
                        return null;
                    }));
                }
                go.countDown();
                for (final Future<?> task : tasks) {
                    task.get(10, TimeUnit.SECONDS);
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertArrayEquals(new Throwable[] { exB }, exA.getSuppressed());
    }

    @Test
    public void c007_freshPerGetSecondariesAreAttachedAtMostOnce() throws Exception {
        final IllegalStateException exB = new IllegalStateException("b");
        final ContinuableFuture<String> b = failed(exB);
        final ContinuableFuture<String> cancelled = cancelledTask();

        for (int i = 0; i < 3; i++) {
            b.callAsyncAfterBoth(cancelled, (x, y) -> x).getAsResult();
            b.runAsyncAfterFirstSuccess(cancelled, () -> {
            }).getAsResult();
        }
        assertEquals(1, exB.getSuppressed().length, "a cancelled sibling (fresh CancellationException per get) is attached once");
        assertInstanceOf(CancellationException.class, exB.getSuppressed()[0]);

        final IllegalStateException exC = new IllegalStateException("c");
        final AssertionError err = new AssertionError("err");
        final ContinuableFuture<String> c = failed(exC);
        final ContinuableFuture<String> bad = failedWithError(err);

        for (int i = 0; i < 3; i++) {
            c.callAsyncAfterBoth(bad, (x, y) -> x).getAsResult();
            c.callAsyncAfterFirstSuccess(bad, x -> x).getAsResult();
        }
        assertArrayEquals(new Throwable[] { err }, exC.getSuppressed(), "an Error sibling is attached as the Error itself, once");
    }

    @Test
    public void c007_combiningInBothOrdersDoesNotBuildACycle() {
        final IllegalStateException exA = new IllegalStateException("a");
        final IllegalStateException exB = new IllegalStateException("b");
        final ContinuableFuture<String> a = failed(exA);
        final ContinuableFuture<String> b = failed(exB);

        a.callAsyncAfterBoth(b, (x, y) -> x).getAsResult();
        b.callAsyncAfterBoth(a, (x, y) -> x).getAsResult();
        b.callAsyncAfterFirstSuccess(a, x -> x).getAsResult();

        assertArrayEquals(new Throwable[] { exB }, exA.getSuppressed());
        assertEquals(0, exB.getSuppressed().length, "exB must not list exA, which already lists exB");
    }

    @Test
    public void c025_aRingOfThreeFailedInputsDoesNotBuildACycle() {
        // C-025: the guard has to follow suppressed links transitively - A -> B -> C -> A is a cycle too.
        final IllegalStateException exA = new IllegalStateException("a");
        final IllegalStateException exB = new IllegalStateException("b");
        final IllegalStateException exC = new IllegalStateException("c");
        final ContinuableFuture<String> a = failed(exA);
        final ContinuableFuture<String> b = failed(exB);
        final ContinuableFuture<String> c = failed(exC);

        a.callAsyncAfterBoth(b, (x, y) -> x).getAsResult();
        b.callAsyncAfterBoth(c, (x, y) -> x).getAsResult();
        c.callAsyncAfterBoth(a, (x, y) -> x).getAsResult();
        c.callAsyncAfterFirstSuccess(a, x -> x).getAsResult();

        assertArrayEquals(new Throwable[] { exB }, exA.getSuppressed());
        assertArrayEquals(new Throwable[] { exC }, exB.getSuppressed());
        assertEquals(0, exC.getSuppressed().length, "exA already reaches exC through exB");
    }

    @Test
    public void c025_concurrentOppositeOrderCombinationsDoNotBuildACycle() throws Exception {
        // C-025: the reachability check and the add are atomic across combinations, so (A,B) and (B,A) racing on
        // one failed pair can never both attach.
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // 2000 rounds: the old lock-free pair check raced about once per 100 rounds, so a miss is ~e^-20.
            for (int round = 0; round < 2000; round++) {
                final IllegalStateException exA = new IllegalStateException("a");
                final IllegalStateException exB = new IllegalStateException("b");
                final ContinuableFuture<String> a = failed(exA);
                final ContinuableFuture<String> b = failed(exB);
                final CountDownLatch go = new CountDownLatch(1);
                final Future<?> ab = pool.submit(() -> {
                    go.await();
                    a.thenUse(DIRECT).callAsyncAfterBoth(b, (x, y) -> x).getAsResult();
                    return null;
                });
                final Future<?> ba = pool.submit(() -> {
                    go.await();
                    b.thenUse(DIRECT).callAsyncAfterBoth(a, (x, y) -> x).getAsResult();
                    return null;
                });
                go.countDown();
                ab.get(10, TimeUnit.SECONDS);
                ba.get(10, TimeUnit.SECONDS);

                final boolean aListsB = exA.getSuppressed().length == 1 && exA.getSuppressed()[0] == exB;
                final boolean bListsA = exB.getSuppressed().length == 1 && exB.getSuppressed()[0] == exA;
                assertTrue(exA.getSuppressed().length <= 1 && exB.getSuppressed().length <= 1, "round " + round + ": at most one entry each");
                assertTrue(aListsB ^ bListsA, "round " + round + ": exactly one direction is recorded, never both");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void c024_timedSingleZipComposeHandsANullHandleToTheZipUnchanged() throws Exception {
        // C-024: the deadline-bounded views must not turn a null handle into a non-null view over nothing; the timed
        // and the untimed get show the zip the same handles (compose's test-locked null-handle contract).
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");
        final CompletableFuture<String> nullHandle = null;

        final ContinuableFuture<String> two = Futures.compose(done, nullHandle, (a, b) -> b == null ? "null handle" : b.get());
        assertEquals("null handle", two.get());
        assertEquals("null handle", two.get(1, TimeUnit.SECONDS));

        final ContinuableFuture<String> three = Futures.compose(done, nullHandle, done, (a, b, c) -> b == null ? a.get() + c.get() : b.get());
        assertEquals("dd", three.get());
        assertEquals("dd", three.get(1, TimeUnit.SECONDS));

        final ContinuableFuture<String> many = Futures.compose(Arrays.asList(done, null), futures -> futures.get(1) == null ? "null handle" : "x");
        assertEquals("null handle", many.get());
        assertEquals("null handle", many.get(1, TimeUnit.SECONDS));
    }

    // ---------------------------------------------------------------- C-008: null input handles are rejected eagerly

    @Test
    public void c008_aggregatesRejectNullInputsBeforeObservingAnything() throws Exception {
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final List<Future<String>> withNull = Arrays.asList(pending, null);

        assertThrows(NullPointerException.class, () -> Futures.allOf(pending, null));
        assertThrows(NullPointerException.class, () -> Futures.allOf(withNull));
        assertThrows(NullPointerException.class, () -> Futures.anyOf(pending, null));
        assertThrows(NullPointerException.class, () -> Futures.anyOf(withNull));
        assertThrows(NullPointerException.class, () -> Futures.combine(pending, null));
        assertThrows(NullPointerException.class, () -> Futures.combine(withNull, values -> "x"));

        // compose keeps its test-locked contract (FutureComposeSnapshotTest): the handles belong to the zip function,
        // a null one reaches it unchanged; only isDone()/isCancelled() have to dereference every handle.
        final List<Future<String>> doneThenNull = Arrays.asList(CompletableFuture.completedFuture("d"), null);
        final ContinuableFuture<String> composed = Futures.compose(doneThenNull, futures -> futures.get(1) == null ? "null handle" : "x");
        assertEquals("null handle", composed.get());
        assertThrows(NullPointerException.class, composed::isDone);

        assertEquals(0, pending.getNumberOfDependents(), "a rejected aggregate must not leave a completion observer behind");
        pending.complete("cleanup");
    }

    // ---------------------------------------------------------------- C-009 / C-014 / C-019: argument errors are argument errors

    @Test
    public void c009_timedGettersRejectANullUnitAsAnArgumentError() {
        final ContinuableFuture<String> future = ContinuableFuture.completed("v");

        assertThrows(IllegalArgumentException.class, () -> future.getAsResult(1, null));
        assertThrows(IllegalArgumentException.class, () -> future.getThenApply(1, null, s -> s));
        assertThrows(IllegalArgumentException.class, () -> future.getThenApply(1, null, (s, e) -> s));
        assertThrows(IllegalArgumentException.class, () -> future.getThenAccept(1, null, s -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> future.getThenAccept(1, null, (s, e) -> {
        }));
        assertThrows(NullPointerException.class, () -> future.get(1, null), "Future.get keeps the JDK contract");
    }

    @Test
    public void c014_thenDelayRejectsANullUnitForAnyDelay() {
        final ContinuableFuture<String> future = ContinuableFuture.completed("v");

        assertThrows(IllegalArgumentException.class, () -> future.thenDelay(0, null));
        assertThrows(IllegalArgumentException.class, () -> future.thenDelay(-1, null));
        assertThrows(IllegalArgumentException.class, () -> future.thenDelay(1, null));
        assertSame(future, future.thenDelay(0, TimeUnit.SECONDS));
    }

    @Test
    public void c016_withIsPrivate() throws Exception {
        // Structural pin: the former package-private @Deprecated internal is now private (no deprecation needed).
        assertTrue(Modifier.isPrivate(ContinuableFuture.class.getDeclaredMethod("with", Executor.class, long.class, TimeUnit.class).getModifiers()));
    }

    @Test
    public void c019_aggregateTimedGetsRejectANullUnitAndVarargsMessagesNameTheParameter() throws Exception {
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");

        for (final ContinuableFuture<?> aggregate : List.of(Futures.allOf(done), Futures.anyOf(done), Futures.compose(List.of(done), futures -> "x"),
                ContinuableFuture.completed("v").thenDelay(1, TimeUnit.DAYS))) {
            final NullPointerException ex = assertThrows(NullPointerException.class, () -> aggregate.get(1, null));
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("unit"), ex.getMessage());
        }
        assertEquals(List.of("d"), Futures.allOf(done).get(-1, TimeUnit.SECONDS), "a non-positive timeout polls");

        for (final Runnable call : List.<Runnable> of(() -> Futures.allOf(new Future[0]), () -> Futures.anyOf(new Future[0]), () -> Futures.iterate(new Future[0]))) {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call::run);
            assertTrue(ex.getMessage().contains("futures"), ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- C-011 / C-012

    @Test
    public void c011_composeReportsAZipErrorAsExecutionException() {
        final CompletableFuture<String> done = CompletableFuture.completedFuture("d");
        final AssertionError err = new AssertionError("zip");
        final ContinuableFuture<String> composed = Futures.compose(done, done, (a, b) -> {
            throw err;
        });

        assertSame(err, assertThrows(ExecutionException.class, composed::get).getCause());
        assertSame(err, assertThrows(ExecutionException.class, () -> composed.get(1, TimeUnit.SECONDS)).getCause());
    }

    @Test
    public void c012_aggregatesRejectASnapshotThatTurnsOutEmpty() {
        final Collection<Future<String>> liar = new AbstractCollection<>() {
            @Override
            public Iterator<Future<String>> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> Futures.allOf(liar));
        assertThrows(IllegalArgumentException.class, () -> Futures.compose(liar, futures -> "x"));
        assertThrows(IllegalArgumentException.class, () -> Futures.anyOf(liar));
        assertThrows(IllegalArgumentException.class, () -> Futures.iterate(liar));
    }

    // ---------------------------------------------------------------- cycle 3: C-032 / C-033

    @Test
    public void c032_registrationStillConsultsIsDoneOfAWrappedCustomInput() {
        // C-032: the cycle-2 reorder skipped isDone() for ContinuableFuture wrappers, so a delegate whose isDone()
        // throws was silently relayed instead of failing registration (and being retryable) as on r9617.
        final IllegalStateException registrationFailure = new IllegalStateException("registration failed");
        final Future<String> custom = new FutureTask<>(() -> "unused") {
            @Override
            public boolean isDone() {
                throw registrationFailure;
            }
        };
        final ContinuableFuture<String> wrapped = ContinuableFuture.wrap(custom).map(v -> v + "!");

        assertSame(registrationFailure, assertThrows(IllegalStateException.class, () -> Futures.iterate(List.of(wrapped))));
        assertSame(registrationFailure, assertThrows(IllegalStateException.class, () -> Futures.anyOf(wrapped).get(1, TimeUnit.SECONDS)));
    }

    @Test
    public void c033_anyOfNonPositiveTimeoutPollCarriesTheObservedFailures() throws Exception {
        // C-033: the poll path threw a bare TimeoutException while the blocking path attached the failures seen so far.
        final IllegalStateException boom = new IllegalStateException("boom");
        final FutureTask<String> failedTask = new FutureTask<>(() -> {
            throw boom;
        });
        failedTask.run();
        final CompletableFuture<String> pending = new CompletableFuture<>();
        try {
            final ContinuableFuture<String> any = Futures.anyOf(failedTask, pending);

            final TimeoutException polled = assertThrows(TimeoutException.class, () -> any.get(0, TimeUnit.NANOSECONDS));
            assertArrayEquals(new Throwable[] { boom }, polled.getSuppressed(), "the poll reports what it observed");

            final TimeoutException waited = assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS));
            assertArrayEquals(new Throwable[] { boom }, waited.getSuppressed());

            final TimeoutException polledAgain = assertThrows(TimeoutException.class, () -> any.get(-1, TimeUnit.SECONDS));
            assertArrayEquals(new Throwable[] { boom }, polledAgain.getSuppressed(), "recorded and observed failures are merged once");

            pending.complete("late");
            assertEquals("late", any.get(1, TimeUnit.SECONDS));
        } finally {
            pending.complete("cleanup");
        }
    }
}
