package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ContinuableFutureThenTest extends ContinuableFutureTestSupport {
    @Test
    public void testThenRunWithRunnable_afterException() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(() -> executed.set(true));

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertEquals("java.lang.RuntimeException: error", ex.getCause().getMessage());
    }

    @Test
    public void testThenRunWithConsumer_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("original error");
        });

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(s -> {
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertEquals("java.lang.RuntimeException: original error", ex.getCause().getMessage());
    }

    @Test
    public void testThenRunWithBiConsumer_handlesException() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        ContinuableFuture<Void> nextFuture = future.thenRunAsync((value, exception) -> {
            ref.set(exception != null ? "ERROR_HANDLED" : value);
        });
        nextFuture.get();

        assertEquals("ERROR_HANDLED", ref.get());
    }

    @Test
    public void testThenCallWithCallable_afterException() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });

        ContinuableFuture<String> nextFuture = future.thenCallAsync(() -> {
            executed.set(true);
            return "recovered";
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertEquals("java.lang.RuntimeException: error", ex.getCause().getMessage());
    }

    @Test
    public void testThenCallWithFunction_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("original error");
        });

        ContinuableFuture<String> nextFuture = future.thenCallAsync(s -> s.toUpperCase());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertEquals("java.lang.RuntimeException: original error", ex.getCause().getMessage());
    }

    @Test
    public void testThenCallWithBiFunction_handlesException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        ContinuableFuture<String> nextFuture = future.thenCallAsync((value, exception) -> {
            return exception != null ? "RECOVERED" : value;
        });

        assertEquals("RECOVERED", nextFuture.get());
    }

    @Test
    public void testThenRunWithRunnable_success() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(() -> executed.set(true));
        nextFuture.get();

        assertTrue(executed.get());
    }

    @Test
    public void testThenRunWithConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(s -> ref.set(s));
        nextFuture.get();

        assertEquals("test", ref.get());
    }

    @Test
    public void testThenRunWithConsumer_withNull() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>("initial");
        ContinuableFuture<String> future = ContinuableFuture.completed(null);

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(s -> ref.set(s == null ? "NULL" : s));
        nextFuture.get();

        assertEquals("NULL", ref.get());
    }

    @Test
    public void testThenRunWithBiConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRunAsync((value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        nextFuture.get();

        assertEquals("test", ref.get());
    }

    @Test
    public void testThenRunWithBiConsumer_nullValue() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed(null);

        ContinuableFuture<Void> nextFuture = future.thenRunAsync((value, exception) -> {
            ref.set(exception == null && value == null ? "NULL" : "NOT_NULL");
        });
        nextFuture.get();

        assertEquals("NULL", ref.get());
    }

    @Test
    public void testThenCallWithCallable_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("first");
        ContinuableFuture<String> nextFuture = future.thenCallAsync(() -> "second");

        assertEquals("second", nextFuture.get());
    }

    @Test
    public void testThenCallWithCallable_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.completed("first");
        ContinuableFuture<String> nextFuture = future.thenCallAsync(() -> {
            throw new IllegalStateException("second error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    public void testThenCallWithFunction_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> nextFuture = future.thenCallAsync(s -> s.toUpperCase());

        assertEquals("TEST", nextFuture.get());
    }

    @Test
    public void testThenCallWithFunction_transformType() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("123");
        ContinuableFuture<Integer> nextFuture = future.thenCallAsync(e -> Integer.parseInt(e));

        assertEquals(123, nextFuture.get());
    }

    @Test
    public void testThenCallWithFunction_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        ContinuableFuture<String> nextFuture = future.thenCallAsync(s -> s == null ? "NULL" : s);

        assertEquals("NULL", nextFuture.get());
    }

    @Test
    public void testThenCallWithBiFunction_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> nextFuture = future.thenCallAsync((value, exception) -> {
            return exception == null ? value.toUpperCase() : "ERROR";
        });

        assertEquals("TEST", nextFuture.get());
    }

    @Test
    public void testThenCallWithBiFunction_nullValue() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        ContinuableFuture<String> nextFuture = future.thenCallAsync((value, exception) -> {
            return exception == null && value == null ? "NULL_HANDLED" : value;
        });

        assertEquals("NULL_HANDLED", nextFuture.get());
    }

    @Test
    public void testThenRunWithRunnable_exceptionInAction() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<Void> nextFuture = future.thenRunAsync(() -> {
            throw new RuntimeException("action error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertTrue(ex.getCause().getMessage().contains("action error"));
    }

    @Test
    public void testThenRunWithConsumer_exceptionInAction() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<Void> nextFuture = future.thenRunAsync(s -> {
            throw new RuntimeException("consumer error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertTrue(ex.getCause().getMessage().contains("consumer error"));
    }

    @Test
    public void testThenCallWithCallable_nullResult() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("first");
        ContinuableFuture<String> nextFuture = future.thenCallAsync(() -> null);

        assertNull(nextFuture.get());
    }

    @Test
    public void testThenCallWithFunction_exceptionInAction() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> nextFuture = future.thenCallAsync(s -> {
            throw new IllegalStateException("function error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay_basic() throws Exception {
        long startTime = System.currentTimeMillis();
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 80);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void testThenDelay_startsAfterSlowUpstreamCompletes() throws Exception {
        final long startNanos = System.nanoTime();
        final ContinuableFuture<String> delayed = ContinuableFuture.call(() -> {
            Thread.sleep(180);
            return "done";
        }).thenDelay(180, TimeUnit.MILLISECONDS);

        assertEquals("done", delayed.get());

        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        assertTrue(elapsedMillis >= 300, "Upstream work and post-completion delay must not overlap; elapsedMillis=" + elapsedMillis);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void testThenDelay_timedGetSharesBudgetBetweenUpstreamAndDelay() throws Exception {
        final ContinuableFuture<String> delayed = ContinuableFuture.call(() -> {
            Thread.sleep(180);
            return "done";
        }).thenDelay(180, TimeUnit.MILLISECONDS);

        assertThrows(TimeoutException.class, () -> delayed.get(260, TimeUnit.MILLISECONDS));
        assertEquals("done", delayed.get());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    public void testThenDelay_appliesAfterExceptionalCompletion() {
        final long startNanos = System.nanoTime();
        final ContinuableFuture<String> delayed = ContinuableFuture.<String> call(() -> {
            Thread.sleep(120);
            throw new IllegalStateException("failed");
        }).thenDelay(140, TimeUnit.MILLISECONDS);

        final ExecutionException exception = assertThrows(ExecutionException.class, delayed::get);
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertEquals("failed", exception.getCause().getMessage());
        assertTrue(elapsedMillis >= 220, "Failure should be exposed after the post-completion delay; elapsedMillis=" + elapsedMillis);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay_appliesAfterUncheckedFailureFromMappedFuture() {
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("value").<String> map(value -> {
            throw new IllegalStateException("mapped failure");
        }).thenDelay(120, TimeUnit.MILLISECONDS);
        final long startNanos = System.nanoTime();

        final ExecutionException exception = assertThrows(ExecutionException.class, delayed::get);
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("mapped failure", exception.getCause().getMessage());
        assertTrue(elapsedMillis >= 80, "Unchecked failure should be exposed after the post-completion delay; elapsedMillis=" + elapsedMillis);
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay_cancellationIsImmediatelyTerminal() {
        final java.util.concurrent.FutureTask<String> pending = new java.util.concurrent.FutureTask<>(() -> "never run");
        final ContinuableFuture<String> delayed = new ContinuableFuture<>(pending).thenDelay(1, TimeUnit.DAYS);

        assertTrue(delayed.cancel(false));
        assertTrue(delayed.isCancelled());
        assertTrue(delayed.isDone());
        assertThrows(CancellationException.class, () -> delayed.get(0, TimeUnit.NANOSECONDS));
        assertThrows(CancellationException.class, delayed::get);
    }

    @Test
    public void testThenDelay_withZeroDelay() throws Exception {
        ContinuableFuture<String> original = ContinuableFuture.completed("test");
        ContinuableFuture<String> delayed = original.thenDelay(0, TimeUnit.MILLISECONDS);

        assertTrue(original == delayed);
        assertEquals("test", delayed.get());
    }

    @Test
    public void testThenDelay_withNegativeDelay() throws Exception {
        ContinuableFuture<String> original = ContinuableFuture.completed("test");
        ContinuableFuture<String> delayed = original.thenDelay(-100, TimeUnit.MILLISECONDS);

        assertTrue(original == delayed);
        assertEquals("test", delayed.get());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay_chainedWithOtherOperations() throws Exception {
        long startTime = System.currentTimeMillis();

        ContinuableFuture<String> future = ContinuableFuture.call(() -> "start").thenDelay(50, TimeUnit.MILLISECONDS).thenCallAsync(s -> s + "-processed");

        assertEquals("start-processed", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 40);
    }

    @Test
    public void testThenDelay_isDone() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(100, TimeUnit.MILLISECONDS);

        // The delay stage is incomplete until the post-completion delay elapses, even if upstream is done.
        assertFalse(future.isDone());
        Thread.sleep(150);
        assertTrue(future.isDone());
        assertEquals("test", future.get());
    }

    @Test
    public void testThenDelay_withException() throws Exception {
        ContinuableFuture<Object> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        }).thenDelay(50, TimeUnit.MILLISECONDS);

        Thread.sleep(100);
        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("error", ex.getCause().getMessage());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testThenDelay_concurrentTimedGetHonorsItsOwnTimeout() throws Exception {
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("done").thenDelay(700, TimeUnit.MILLISECONDS);
        final CountDownLatch monitorHeld = new CountDownLatch(1);
        final CountDownLatch releaseMonitor = new CountDownLatch(1);
        final CountDownLatch timedGetReturned = new CountDownLatch(1);
        final AtomicBoolean timedOut = new AtomicBoolean();
        final AtomicReference<Throwable> timedGetFailure = new AtomicReference<>();
        final Thread monitorBlocker = new Thread(() -> {
            synchronized (delayed.future) {
                monitorHeld.countDown();

                try {
                    releaseMonitor.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        final Thread timedGetter = new Thread(() -> {
            try {
                delayed.get(40, TimeUnit.MILLISECONDS);
                timedGetFailure.set(new AssertionError("Expected TimeoutException"));
            } catch (final TimeoutException e) {
                timedOut.set(true);
            } catch (final Throwable e) {
                timedGetFailure.set(e);
            } finally {
                timedGetReturned.countDown();
            }
        });

        try {
            monitorBlocker.start();
            assertTrue(monitorHeld.await(1, TimeUnit.SECONDS));
            timedGetter.start();

            // The timeout must be honored while another thread holds the wrapper monitor.
            // The old implementation slept while synchronized on that monitor and remained blocked.
            assertTrue(timedGetReturned.await(2, TimeUnit.SECONDS));
            assertTrue(timedOut.get());
            assertNull(timedGetFailure.get());
        } finally {
            releaseMonitor.countDown();
            monitorBlocker.join(2000);
            timedGetter.join(2000);
        }

        assertFalse(monitorBlocker.isAlive());
        assertFalse(timedGetter.isAlive());
    }

    @Test
    public void testThenDelay_cancel() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "test";
        }).thenDelay(500, TimeUnit.MILLISECONDS);

        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());
    }

    @Test
    public void testThenUse_basic() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenUse(customExecutor).thenCallAsync(() -> {
            threadRef.set(Thread.currentThread());
            return "executed";
        });

        assertEquals("executed", future.get());
        assertNotNull(threadRef.get());
    }

    @Test
    public void testThenUse_switchExecutors() throws Exception {
        Executor executor1 = Executors.newSingleThreadExecutor();
        Executor executor2 = Executors.newFixedThreadPool(2);
        AtomicReference<Thread> thread1 = new AtomicReference<>();
        AtomicReference<Thread> thread2 = new AtomicReference<>();

        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            thread1.set(Thread.currentThread());
            return "step1";
        }, executor1).thenUse(executor2).thenCallAsync(() -> {
            thread2.set(Thread.currentThread());
            return "step2";
        });

        assertEquals("step2", future.get());
        assertNotNull(thread1.get());
        assertNotNull(thread2.get());
    }

    @Test
    public void testThenUse_withNullExecutor() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        assertThrows(IllegalArgumentException.class, () -> future.thenUse(null));
    }

    @Test
    public void testThenUse_chainedOperations() throws Exception {
        Executor customExecutor = Executors.newFixedThreadPool(2);

        ContinuableFuture<Integer> future = ContinuableFuture.call(() -> 1).thenUse(customExecutor).thenCallAsync(v -> v + 1).thenCallAsync(v -> v * 2);

        assertEquals(4, future.get());
    }

    @Test
    public void testThenUse_preservesValue() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.completed("preserved");
        ContinuableFuture<String> withExecutor = future.thenUse(customExecutor);

        assertEquals("preserved", withExecutor.get());
    }

    @Test
    @Timeout(10)
    public void testThenDelay_remainingDelayHonoredAfterShortTimedGet() throws Exception {
        final long delayMillis = 400;
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("done").thenDelay(delayMillis, TimeUnit.MILLISECONDS);

        // A timed get() with a timeout much shorter than the configured delay must
        // time out (the artificial delay is capped at the timeout).
        assertThrows(TimeoutException.class, () -> delayed.get(20, TimeUnit.MILLISECONDS));

        // The configured delay was only partially consumed by the capped wait above.
        // A subsequent get() MUST still honor the remaining delay before returning.
        final long startNanos = System.nanoTime();
        final String result = delayed.get();
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertEquals("done", result);
        // Allow generous slack for scheduling jitter but require that a substantial
        // portion of the remaining delay was actually applied. With the pre-fix logic
        // (isDelayed flag set before the capped sleep), this get() returned almost
        // immediately and elapsedMillis would be ~0.
        assertTrue(elapsedMillis >= delayMillis / 2, "Remaining delay was not honored after a short timed get(); elapsedMillis=" + elapsedMillis);
    }

    @Test
    public void testThenDelayPropagatesCancelAll() throws Exception {
        // regression: with() (thenDelay/thenUse) returned a future with no-op cancelAll and
        // isAllCancelled overrides and null upFutures, silently severing chain cancellation
        final ContinuableFuture<String> f1 = ContinuableFuture.call(() -> {
            Thread.sleep(5000);
            return "step1";
        });
        final ContinuableFuture<String> f2 = f1.thenCallAsync(s -> s + "-2");
        final ContinuableFuture<String> f3 = f2.thenDelay(100, TimeUnit.MILLISECONDS);

        f3.cancelAll(true);

        assertTrue(f1.isCancelled(), "upstream future must be cancelled through thenDelay");
        assertTrue(f3.isAllCancelled());
    }
}
