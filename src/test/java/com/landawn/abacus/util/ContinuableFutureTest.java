package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;

public class ContinuableFutureTest extends ContinuableFutureTestSupport {
    @Test
    @Timeout(10)
    public void testNamingMapFailureRecoveryOverloads() throws Exception {
        final CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("first"));
        final CompletableFuture<String> otherFailed = new CompletableFuture<>();
        otherFailed.completeExceptionally(new IllegalArgumentException("second"));
        final ContinuableFuture<String> first = ContinuableFuture.wrap(failed).thenUse(Runnable::run);
        final ContinuableFuture<String> second = ContinuableFuture.wrap(otherFailed);
        final AtomicInteger callbacks = new AtomicInteger();

        assertNull(first.thenRunAsync((value, failure) -> {
            assertNull(value);
            assertNotNull(failure);
            callbacks.incrementAndGet();
        }).get());
        assertNull(first.runAsyncAfterEither(second, () -> {
            callbacks.incrementAndGet();
        }).get());
        assertEquals("recovered", first.callAsyncAfterEither(second, () -> "recovered").get());

        assertNull(first.runAsyncAfterFirstSuccess(second, (value, failure) -> {
            assertNull(value);
            assertNotNull(failure);
            callbacks.incrementAndGet();
        }).get());
        assertEquals("recovered", first.callAsyncAfterFirstSuccess(second, (value, failure) -> {
            assertNull(value);
            assertNotNull(failure);
            return "recovered";
        }).get());

        assertThrows(ExecutionException.class, () -> first.runAsyncAfterFirstSuccess(second, () -> {
            callbacks.incrementAndGet();
        }).get());
        assertThrows(ExecutionException.class, () -> first.callAsyncAfterFirstSuccess(second, () -> "unreachable").get());
        assertEquals(3, callbacks.get());
    }

    @Test
    @Timeout(2)
    public void testDelayedWrapperNegativeExtremeTimeoutDoesNotOverflow() throws Exception {
        final java.util.concurrent.FutureTask<String> pending = new java.util.concurrent.FutureTask<>(() -> "never run");
        final ContinuableFuture<String> future = new ContinuableFuture<>(pending).thenUse(Runnable::run);

        assertThrows(TimeoutException.class, () -> future.get(Long.MIN_VALUE, TimeUnit.NANOSECONDS));

        final ContinuableFuture<String> completed = ContinuableFuture.wrap(CompletableFuture.completedFuture("done")).thenUse(Runnable::run);
        assertEquals("done", completed.get(Long.MIN_VALUE, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testCancellation_propagatesInChain() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCallAsync(s -> {
            Thread.sleep(200);
            return s + "2";
        });

        ContinuableFuture<String> future3 = future2.thenCallAsync(s -> s + "3");

        future3.cancelAll(true);
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
        assertTrue(future3.isCancelled());
    }

    @Test
    public void testGett_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.getAsResult();

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("success", result.orElseThrow());
        assertNull(result.getException());
    }

    @Test
    public void testGett_withException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        Result<String, Exception> result = future.getAsResult();
        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertNotNull(result.getException());
        assertTrue(result.getException() instanceof RuntimeException);
    }

    @Test
    public void testGett_withCancellation() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "should be cancelled";
        });

        future.cancel(true);
        Result<String, Exception> result = future.getAsResult();

        assertTrue(result.isFailure());
        assertTrue(result.getException() instanceof CancellationException);
    }

    @Test
    public void testGett_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> null);
        Result<String, Exception> result = future.getAsResult();

        assertTrue(result.isSuccess());
        assertNull(result.orElseThrow());
    }

    @Test
    public void testGettWithTimeout_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.getAsResult(1, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertEquals("success", result.orElseThrow());
    }

    @Test
    public void testGettWithTimeout_timeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "too late";
        });

        Result<String, Exception> result = future.getAsResult(100, TimeUnit.MILLISECONDS);
        assertTrue(result.isFailure());
        assertTrue(result.getException() instanceof TimeoutException);
        future.cancel(true);
    }

    @Test
    public void testGettWithTimeout_exception() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalStateException("state error");
        });

        Result<String, Exception> result = future.getAsResult(1, TimeUnit.SECONDS);
        assertTrue(result.isFailure());
        assertNotNull(result.getException());
    }

    @Test
    public void testComplexChain_success() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            counter.incrementAndGet();
            return "1";
        }).thenCallAsync(s -> {
            counter.incrementAndGet();
            return s + "2";
        }).thenDelay(50, TimeUnit.MILLISECONDS).thenCallAsync(s -> {
            counter.incrementAndGet();
            return s + "3";
        });

        assertEquals("123", future.get());
        assertEquals(3, counter.get());
    }

    @Test
    public void testComplexChain_withErrors() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();

        ContinuableFuture<Void> future = ContinuableFuture.call(() -> "start").thenCallAsync(s -> {
            if (s.equals("start")) {
                throw new RuntimeException("intentional error");
            }
            return s;
        }).thenRunAsync((value, exception) -> {
            if (exception != null) {
                ref.set("caught: " + exception.getMessage());
            } else {
                ref.set("value: " + value);
            }
        });

        future.get();
        assertEquals("caught: intentional error", ref.get());
    }

    @Test
    public void testComplexChain_multipleUpstreams() throws Exception {
        ContinuableFuture<Integer> future1 = ContinuableFuture.call(() -> 10);
        ContinuableFuture<Integer> future2 = ContinuableFuture.call(() -> 20);
        ContinuableFuture<Integer> future3 = ContinuableFuture.call(() -> 30);

        ContinuableFuture<Integer> combined1 = future1.callAsyncAfterBoth(future2, (v1, v2) -> v1 + v2);
        ContinuableFuture<Integer> combined2 = combined1.callAsyncAfterBoth(future3, (v1, v2) -> v1 + v2);

        assertEquals(60, combined2.get());
    }

    @Test
    public void testNestedFutures_success() throws Exception {
        ContinuableFuture<String> outer = ContinuableFuture.call(() -> {
            ContinuableFuture<String> inner = ContinuableFuture.call(() -> "inner");
            return inner.get() + "-outer";
        });

        assertEquals("inner-outer", outer.get());
    }

    @Test
    public void testTimeout_inChain() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "slow";
        }).thenCallAsync(s -> s + "-processed");

        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
        future.cancel(true);
    }

    @Test
    public void testExceptionHandling_biConsumer() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ContinuableFuture<Void> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        }).thenRunAsync((value, exception) -> {
            if (exception != null) {
                errorCount.incrementAndGet();
            } else {
                successCount.incrementAndGet();
            }
        });

        future.get();
        assertEquals(0, successCount.get());
        assertEquals(1, errorCount.get());
    }

    @Test
    public void testParallelExecution_afterBoth() throws Exception {
        long startTime = System.currentTimeMillis();

        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "A";
        });

        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "B";
        });

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, v2) -> v1 + v2);
        assertEquals("AB", combined.get());

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 180);
    }

    @Test
    public void testInterruption_handling() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            latch.await();
            return "should not complete";
        });

        Thread currentThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(100);
                currentThread.interrupt();
            } catch (InterruptedException e) {
            }
        });
        interrupter.start();

        assertThrows(InterruptedException.class, () -> future.get());
        latch.countDown();
    }

    @Test
    public void testChainedCancellation_partial() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCallAsync(s -> s + "2");

        future2.cancel(true);

        assertTrue(future2.isCancelled());
        assertFalse(future2.isAllCancelled());
        future1.cancel(true);
    }

    @Test
    public void testExecutionException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test exception");
        });

        assertThrows(ExecutionException.class, () -> future.get());
    }

    @Test
    public void testBothFuturesFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, () -> {
        });

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testCompleted_cannotCancel() {
        ContinuableFuture<String> future = ContinuableFuture.completed("value");

        assertFalse(future.cancel(true));
        assertFalse(future.cancel(false));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testCompleted_basic() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("completed value");

        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals("completed value", future.get());
    }

    @Test
    public void testCompleted_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);

        assertTrue(future.isDone());
        assertNull(future.get());
    }

    @Test
    public void testCompleted_getWithTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("immediate");
        assertEquals("immediate", future.get(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCompleted_getWithTimeoutRejectsNullUnit() {
        ContinuableFuture<String> future = ContinuableFuture.completed("immediate");

        assertThrows(NullPointerException.class, () -> future.get(1, null));
    }

    @Test
    public void testMultipleCombinations_eitherAndBoth() throws Exception {
        ContinuableFuture<String> f1 = ContinuableFuture.completed("A");
        ContinuableFuture<String> f2 = ContinuableFuture.completed("B");
        ContinuableFuture<String> f3 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "C";
        });

        ContinuableFuture<String> either = f1.callAsyncAfterEither(f2, s -> s);
        ContinuableFuture<String> both = either.callAsyncAfterBoth(f3, (v1, v2) -> v1 + v2);

        String result = both.get();
        assertTrue(result.equals("AC") || result.equals("BC"));
    }

    @Test
    public void testCompletedFuture_multipleGets() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("value");

        assertEquals("value", future.get());
        assertEquals("value", future.get(1, TimeUnit.MILLISECONDS));
        assertEquals("value", future.getNow("default"));

        Result<String, Exception> result = future.getAsResult();
        assertTrue(result.isSuccess());
        assertEquals("value", result.orElseThrow());
    }

    @Test
    public void testTupleAccess_inCallbacks() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();

        ContinuableFuture<String> f1 = ContinuableFuture.completed("left");
        ContinuableFuture<Integer> f2 = ContinuableFuture.completed(42);

        f1.runAsyncAfterBoth(f2, tuple -> {
            ref.set(tuple._1 + ":" + tuple._3);
        }).get();

        assertEquals("left:42", ref.get());
    }

    @Test
    public void testWrap_completedFuture() throws Exception {
        Future<String> standardFuture = CompletableFuture.completedFuture("wrapped");
        ContinuableFuture<String> future = ContinuableFuture.wrap(standardFuture);

        assertEquals("wrapped", future.get());
        assertTrue(future.isDone());
    }

    @Test
    public void testWrap_pendingFuture() throws Exception {
        CompletableFuture<Integer> standardFuture = new CompletableFuture<>();
        ContinuableFuture<Integer> future = ContinuableFuture.wrap(standardFuture);

        assertFalse(future.isDone());
        standardFuture.complete(42);
        assertEquals(42, future.get());
    }

    @Test
    public void testWrap_cancelledFuture() {
        CompletableFuture<String> standardFuture = new CompletableFuture<>();
        standardFuture.cancel(true);
        ContinuableFuture<String> future = ContinuableFuture.wrap(standardFuture);

        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, () -> future.get());
    }

    @Test
    public void testWrap_withException() {
        CompletableFuture<String> standardFuture = new CompletableFuture<>();
        standardFuture.completeExceptionally(new RuntimeException("wrapped error"));
        ContinuableFuture<String> future = ContinuableFuture.wrap(standardFuture);

        assertTrue(future.isDone());
        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("wrapped error", ex.getCause().getMessage());
    }

    @Test
    public void testWrapRejectsNullFuture() {
        assertThrows(IllegalArgumentException.class, () -> ContinuableFuture.wrap(null));
    }

    @Test
    public void testCancel_withMayInterruptIfRunning() {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            latch.await();
            return "interrupted";
        });

        assertTrue(future.cancel(false));
        assertTrue(future.isCancelled());
        latch.countDown();
    }

    @Test
    public void testCancel_pendingTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            latch.await();
            return "should not complete";
        });

        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        assertThrows(CancellationException.class, () -> future.get());
        latch.countDown();
    }

    @Test
    public void testCancel_completedTask() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("done");
        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testIsCancelled_notCancelled() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "test");
        assertFalse(future.isCancelled());
    }

    @Test
    public void testIsCancelled_afterCancel() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });

        future.cancel(true);
        assertTrue(future.isCancelled());
    }

    @Test
    public void testCancelAll_singleFuture() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });

        assertTrue(future.cancelAll(true));
        assertTrue(future.isCancelled());
    }

    @Test
    public void testCancelAll_chainedFutures() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCallAsync(() -> {
            Thread.sleep(100);
            return "2";
        });

        assertTrue(future2.cancelAll(true));
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
    }

    @Test
    public void testCancelAll_multipleUpstreams() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "2";
        });

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, v2) -> v1 + v2);

        if (combined.cancelAll(true)) {
            assertTrue(future1.isCancelled());
            assertTrue(future2.isCancelled());
            assertTrue(combined.isCancelled());
        } else {
            assertFalse(combined.isCancelled());
            assertFalse(future1.isCancelled() && future2.isCancelled());
        }
    }

    @Test
    public void testIsAllCancelled_notCancelled() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        assertFalse(future.isAllCancelled());
    }

    @Test
    public void testIsAllCancelled_allCancelled() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCallAsync(() -> "2");

        future2.cancelAll(true);
        assertTrue(future2.isAllCancelled());
    }

    @Test
    public void testIsAllCancelled_partialCancellation() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCallAsync(s -> s + "2");

        future2.cancel(true);
        assertFalse(future2.isAllCancelled());
    }

    @Test
    public void testIsDone_pending() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(5000);
            return "pending";
        });
        assertFalse(future.isDone());
        future.cancel(true);
    }

    @Test
    public void testIsDone_cancelled() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });
        future.cancel(true);
        assertTrue(future.isDone());
    }

    @Test
    public void testIsDone_completed() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "done");
        Thread.sleep(50);
        assertTrue(future.isDone());
    }

    @Test
    public void testMap_cancel() {
        ContinuableFuture<String> original = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });
        ContinuableFuture<String> mapped = original.map(s -> s.toUpperCase());

        assertTrue(mapped.cancel(true));
        assertTrue(mapped.isCancelled());
    }

    @Test
    public void testMap_cancelAll() {
        ContinuableFuture<String> original = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });
        ContinuableFuture<String> mapped = original.map(s -> s.toUpperCase());

        assertTrue(mapped.cancelAll(true));
        assertTrue(original.isCancelled());
        assertTrue(mapped.isCancelled());
    }

    @Test
    public void testMap_isDone_beforeCompletion() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(5000);
            return "pending";
        });
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertFalse(mapped.isDone());
        future.cancel(true);
    }

    @Test
    public void testMap_isCancelled() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "test";
        });
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertFalse(mapped.isCancelled());
        mapped.cancel(true);
        assertTrue(mapped.isCancelled());
    }

    @Test
    public void testMap_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertEquals("TEST", mapped.get());
    }

    @Test
    public void testMap_transformType() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("123");
        ContinuableFuture<Integer> mapped = future.map(Integer::parseInt);

        assertEquals(123, mapped.get());
    }

    @Test
    public void testMap_withException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> mapped = future.map(s -> {
            throw new IllegalArgumentException("mapping error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> mapped.get());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("mapping error", ex.getCause().getMessage());
        ExecutionException timed = assertThrows(ExecutionException.class, () -> mapped.get(1, TimeUnit.SECONDS));
        assertTrue(timed.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void testMap_chainedMaps() throws Exception {
        ContinuableFuture<Integer> future = ContinuableFuture.completed(5);
        ContinuableFuture<String> mapped = future.map(i -> i * 2).map(i -> "Value: " + i);

        assertEquals("Value: 10", mapped.get());
    }

    @Test
    public void testMap_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        ContinuableFuture<String> mapped = future.map(s -> s == null ? "NULL" : s);

        assertEquals("NULL", mapped.get());
    }

    @Test
    public void testMap_getWithTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertEquals("TEST", mapped.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMap_getWithTimeout_timesOut() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "slow";
        });
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertThrows(TimeoutException.class, () -> mapped.get(100, TimeUnit.MILLISECONDS));
        future.cancel(true);
    }

    @Test
    public void testObservableFuture() throws Exception {
        AsyncExecutor asyncExecutor = new AsyncExecutor();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);
        AtomicReference<Exception> error = new AtomicReference<>();

        asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> ran.set(true)).thenRunAsync((BiConsumer<Void, Exception>) (value, e) -> {
            error.set(e);
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertTrue(ran.get());
        assertNull(error.get());
    }

    @Test
    public void testObservableFuture_withException() throws Exception {
        AsyncExecutor asyncExecutor = new AsyncExecutor();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> {
            throw new RuntimeException("observable failure");
        }).thenRunAsync((BiConsumer<Void, Exception>) (value, e) -> {
            error.set(e);
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertNotNull(error.get());
        assertEquals("observable failure", error.get().getMessage());
    }

    @Test
    public void testObservableFuture_manyTasks() throws Exception {
        AsyncExecutor asyncExecutor = new AsyncExecutor(8, 16, 300, TimeUnit.SECONDS);
        int taskCount = 20;
        CountDownLatch done = new CountDownLatch(taskCount);
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < taskCount; i++) {
            asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> {
                throw new RuntimeException("boom");
            }).thenRunAsync((BiConsumer<Void, Exception>) (value, e) -> {
                if (e != null) {
                    errors.incrementAndGet();
                }
                done.countDown();
            });
        }

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, errors.get());
    }

    @Test
    public void testCallbackExecute() throws Exception {
        AsyncExecutor asyncExecutor = new AsyncExecutor(8, 16, 300, TimeUnit.SECONDS);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();

        asyncExecutor.execute((Callable<String>) () -> {
            throw new RuntimeException("callback failure");
        }).thenCallAsync((BiFunction<String, Exception, String>) (value, e) -> {
            assertNotNull(e);
            return "abc";
        }).thenRunAsync((BiConsumer<String, Exception>) (value, e) -> {
            result.set(value);
            error.set(e);
            done.countDown();
        });

        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals("abc", result.get());
        assertNull(error.get());
    }

    @Test
    public void testThenDelayThenCallAsync() throws Exception {
        long start = System.currentTimeMillis();
        ContinuableFuture<String> future = N.asyncExecute((Throwables.Runnable<RuntimeException>) () -> {
        }).thenDelay(100, TimeUnit.MILLISECONDS).thenCallAsync((Callable<String>) () -> "second");

        assertEquals("second", future.get(2, TimeUnit.SECONDS));
        assertTrue(System.currentTimeMillis() - start >= 80);
    }

    @Test
    public void testToCompletableFutureWithExecutor_rejectsNullExecutor() {
        ContinuableFuture<String> future = ContinuableFuture.completed("v");

        assertThrows(IllegalArgumentException.class, () -> future.toCompletableFuture((Executor) null));
    }

    @Test
    public void testToCompletableFuture_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("hello");
        CompletableFuture<String> cf = future.toCompletableFuture();

        assertEquals("hello", cf.get());
    }

    @Test
    public void testToCompletableFuture_withException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("cf error");
        });
        CompletableFuture<String> cf = future.toCompletableFuture();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> cf.get());
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("cf error", ex.getCause().getMessage());
        assertFalse(ex.getCause() instanceof ExecutionException);
    }

    @Test
    public void testToCompletableFuture_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        CompletableFuture<String> cf = future.toCompletableFuture();

        assertNull(cf.get());
    }

    @Test
    public void testToCompletableFuture_chainWithCompletableFuture() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("start");
        CompletableFuture<String> cf = future.toCompletableFuture();
        CompletableFuture<String> chained = cf.thenApply(s -> s + "-chained");

        assertEquals("start-chained", chained.get());
    }

    @Test
    public void testToCompletableFutureWithExecutor_success() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.completed("with executor");
        CompletableFuture<String> cf = future.toCompletableFuture(customExecutor);

        assertEquals("with executor", cf.get());
    }

    @Test
    public void testToCompletableFutureWithExecutor_withException() {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("executor cf error");
        });
        CompletableFuture<String> cf = future.toCompletableFuture(customExecutor);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> cf.get());
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("executor cf error", ex.getCause().getMessage());
        assertFalse(ex.getCause() instanceof ExecutionException);
    }

    @Test
    public void testToCompletableFutureWithExecutor_withNull() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        CompletableFuture<String> cf = future.toCompletableFuture(customExecutor);

        assertNull(cf.get());
    }

    @Test
    public void testToCompletableFutureWithExecutor_chainWithCompletableFuture() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<Integer> future = ContinuableFuture.completed(42);
        CompletableFuture<String> cf = future.toCompletableFuture(customExecutor).thenApply(n -> "answer: " + n);

        assertEquals("answer: 42", cf.get());
    }

    @Test
    public void testToCompletableFuture_thenAcceptAsync() throws Exception {
        AtomicReference<String> accepted = new AtomicReference<>();
        ContinuableFuture.call(() -> "Hello").toCompletableFuture().thenAccept(accepted::set).get(2, TimeUnit.SECONDS);
        assertEquals("Hello", accepted.get());

        CompletableFuture<String> async = ContinuableFuture.call(() -> "world").toCompletableFuture().thenApplyAsync(s -> s);
        assertEquals("world", async.get(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSubMillisecondTimedGetDoesNotBypassDelay() {
        // regression: a sub-millisecond timeout truncated to 0 ms skipped the delay gate entirely
        // and returned the (already completed) value long before the configured delay elapsed
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("x").thenDelay(10, TimeUnit.SECONDS);

        assertThrows(java.util.concurrent.TimeoutException.class, () -> delayed.get(900, TimeUnit.MICROSECONDS));
    }

    @Test
    public void testNegativeTimedGetOnDelayedFutureTimesOut() {
        final ContinuableFuture<String> delayed = ContinuableFuture.completed("x").thenDelay(10, TimeUnit.SECONDS);

        assertThrows(TimeoutException.class, () -> delayed.get(-1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void reviewFixes20260906_tuple4CallbacksExposeTheExceptionSlots() throws Exception {
        // The two Tuple4 combinators declared `? super Exception` INSIDE the tuple, so tuple._2 / tuple._4
        // captured to CAP#1 and were only readable as Object - their own javadoc examples did not compile, and
        // no test in the tree ever read them. This is that example, made executable.
        final ContinuableFuture<String> ok1 = ContinuableFuture.call(() -> "a");
        final ContinuableFuture<String> ok2 = ContinuableFuture.call(() -> "b");

        final ContinuableFuture<String> combined = ok1.callAsyncAfterBoth(ok2, tuple -> {
            final String primary = tuple._1;
            final Exception primaryError = tuple._2;
            final String backup = tuple._3;
            final Exception backupError = tuple._4;

            return primary + backup + "/" + primaryError + "/" + backupError;
        });

        assertEquals("ab/null/null", combined.get());

        // The failing shape: both exceptions must arrive typed.
        final ContinuableFuture<String> bad1 = ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom1");
        });
        final ContinuableFuture<String> bad2 = ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom2");
        });

        final java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();

        bad1.runAsyncAfterBoth(bad2, tuple -> {
            final Exception e1 = tuple._2;
            final Exception e2 = tuple._4;

            seen.set(CommonUtil.toString(e1 == null ? null : e1.getMessage()) + "," + CommonUtil.toString(e2 == null ? null : e2.getMessage()));
        }).get();

        assertNotNull(seen.get());
        assertTrue(seen.get().contains("boom1"), seen.get());
        assertTrue(seen.get().contains("boom2"), seen.get());
    }

    @Test
    public void reviewFixes20260906_combinatorsValidateOtherEagerly() {
        // `other` is documented "must not be null" on the combinators but was never validated: the NPE surfaced
        // asynchronously inside the executor, and execute(futureTask, null) silently modelled a ONE-input chain,
        // so cancelAll()/isAllCancelled() described something the caller never asked for.
        final ContinuableFuture<String> f = ContinuableFuture.completed("x");

        assertThrows(IllegalArgumentException.class, () -> f.callAsyncAfterBoth(null, (a, b) -> a));
        assertThrows(IllegalArgumentException.class, () -> f.runAsyncAfterBoth(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> f.callAsyncAfterEither(null, () -> "y"));
        assertThrows(IllegalArgumentException.class, () -> f.runAsyncAfterEither(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> f.callAsyncAfterFirstSuccess(null, () -> "y"));
        assertThrows(IllegalArgumentException.class, () -> f.runAsyncAfterFirstSuccess(null, () -> {
        }));

        // `action` is still validated too. (The cast picks one of the two overloads: a bare `null` is
        // ambiguous between the Tuple4 and Quad forms, as it always has been.)
        assertThrows(IllegalArgumentException.class, () -> f.callAsyncAfterBoth(ContinuableFuture.completed("y"),
                (Throwables.Function<? super Tuple.Tuple4<String, Exception, String, Exception>, String, ? extends Exception>) null));
    }

    @Test
    public void reviewFixes20260906_bothFailuresAreSuppressedOnlyOnce() throws Exception {
        // throwIfEitherFailed and the *AfterFirstSuccess family record the second failure as suppressed on the
        // FIRST one - an exception owned by the input future, which outlives the combination. Combining the same
        // failed pair repeatedly used to append the same suppressed entry again and again.
        final IllegalStateException exA = new IllegalStateException("a");
        final IllegalStateException exB = new IllegalStateException("b");

        final ContinuableFuture<String> a = ContinuableFuture.call(() -> {
            throw exA;
        });
        final ContinuableFuture<String> b = ContinuableFuture.call(() -> {
            throw exB;
        });

        a.getAsResult();
        b.getAsResult();

        for (int i = 0; i < 3; i++) {
            a.callAsyncAfterBoth(b, (x, y) -> x).getAsResult();
        }

        assertEquals(1, exA.getSuppressed().length);
        org.junit.jupiter.api.Assertions.assertSame(exB, exA.getSuppressed()[0]);
    }

    @Test
    public void reviewFixes20260906_cancellingAnAlwaysRunStageDoesNotFabricateFailures() throws Exception {
        // The four "always-run" *AfterBoth overloads read their inputs with getAsResult(), which turns the
        // WORKER's own InterruptedException into "this future failed" and restores the interrupt flag - which
        // then makes the very next get() fail instantly. Cancelling the combined stage therefore invoked the
        // callback with two fabricated InterruptedExceptions for two futures that had not completed at all,
        // contradicting the javadoc ("if a future fails, its result is null and its exception is non-null").
        // awaitResult rethrows the interruption instead, so the stage just ends.
        for (final boolean quad : new boolean[] { false, true }) {
            final java.util.concurrent.CompletableFuture<String> neverA = new java.util.concurrent.CompletableFuture<>();
            final java.util.concurrent.CompletableFuture<String> neverB = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> fa = ContinuableFuture.wrap(neverA);
            final ContinuableFuture<String> fb = ContinuableFuture.wrap(neverB);

            final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<Object[]> seen = new java.util.concurrent.atomic.AtomicReference<>();

            // The stage cannot tell us it started, so give the worker a moment to park in get().
            final ContinuableFuture<Void> combined = quad ? fa.runAsyncAfterBoth(fb, (v1, e1, v2, e2) -> {
                seen.set(new Object[] { v1, e1, v2, e2 });
                entered.countDown();
            }) : fa.runAsyncAfterBoth(fb, tuple -> {
                seen.set(new Object[] { tuple._1, tuple._2, tuple._3, tuple._4 });
                entered.countDown();
            });

            Thread.sleep(120);
            assertTrue(combined.cancel(true));
            assertTrue(!entered.await(1, java.util.concurrent.TimeUnit.SECONDS),
                    "the callback must not run for two futures that never completed (quad=" + quad + ")");
            assertNull(seen.get(), "no fabricated results/exceptions may reach the callback (quad=" + quad + ")");

            neverA.complete("a");
            neverB.complete("b");
        }
    }

    @Test
    public void reviewFixes20260906_alwaysRunStageStillReportsGenuineInputFailures() throws Exception {
        // The other half of the same contract: a real input failure must still reach the callback as a
        // non-null exception, and a real success as a non-null value.
        final ContinuableFuture<String> ok = ContinuableFuture.call(() -> "v");
        final ContinuableFuture<String> bad = ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom");
        });

        final java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();
        ok.runAsyncAfterBoth(bad,
                tuple -> seen.set(tuple._1 + "/" + (tuple._2 == null) + "/" + (tuple._3 == null) + "/" + (tuple._4 == null ? "null" : tuple._4.getMessage())))
                .get();
        assertEquals("v/true/true/boom", seen.get());

        final java.util.concurrent.atomic.AtomicReference<String> seenQuad = new java.util.concurrent.atomic.AtomicReference<>();
        ContinuableFuture.call(() -> "v").runAsyncAfterBoth(ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom2");
        }), (v1, e1, v2, e2) -> seenQuad.set(v1 + "/" + (e1 == null) + "/" + (v2 == null) + "/" + (e2 == null ? "null" : e2.getMessage()))).get();
        assertEquals("v/true/true/boom2", seenQuad.get());

        // ... and the callAsync twins behave the same way.
        assertEquals("v-boom3", ContinuableFuture.call(() -> "v").callAsyncAfterBoth(ContinuableFuture.call(() -> {
            throw new IllegalStateException("boom3");
        }), tuple -> tuple._1 + "-" + (tuple._4 == null ? "null" : tuple._4.getMessage())).get());
    }

    @Test
    @Timeout(20)
    public void reviewFixes20260906_toCompletableFutureDoesNotPropagateCancellationAsCancellation() throws Exception {
        // Documented (F5): the javadoc's "CancellationException: Propagated as cancellation" bullet was wrong,
        // and contradicted the "Cancelling this ContinuableFuture will cause the CompletableFuture to complete
        // exceptionally" bullet six lines below it. getForCompletableFuture() catches only Interrupted/Execution,
        // so the CancellationException escapes as a plain RuntimeException and CompletableFuture stores it under
        // a CompletionException - which is not a CancellationException, so isCancelled() stays false.
        final java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
            final Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        try {
            final CountDownLatch release = new CountDownLatch(1);
            final ContinuableFuture<String> f = ContinuableFuture.call(() -> {
                release.await(10, TimeUnit.SECONDS);
                return "x";
            });

            final CompletableFuture<String> cf = f.toCompletableFuture(executor);
            Thread.sleep(150); // let the conversion task park in get()
            assertTrue(f.cancel(true));

            final ExecutionException ee = assertThrows(ExecutionException.class, () -> cf.get(10, TimeUnit.SECONDS));
            assertTrue(ee.getCause() instanceof CancellationException, "got " + ee.getCause());
            assertFalse(cf.isCancelled(), "the CompletableFuture is NOT itself cancelled");
            assertTrue(cf.isCompletedExceptionally());
            assertTrue(f.isCancelled(), "the source ContinuableFuture is the one that is cancelled");

            final java.util.concurrent.CompletionException ce = assertThrows(java.util.concurrent.CompletionException.class, cf::join);
            assertTrue(ce.getCause() instanceof CancellationException);

            release.countDown();

            // Control - a genuine task failure surfaces the same shape (ExecutionException with the real cause),
            // and it too leaves isCancelled() false, so the two cases are indistinguishable by isCancelled().
            final CompletableFuture<String> failing = ContinuableFuture.<String> call(() -> {
                throw new java.io.IOException("boom");
            }).toCompletableFuture(executor);
            final ExecutionException fail = assertThrows(ExecutionException.class, () -> failing.get(10, TimeUnit.SECONDS));
            assertTrue(fail.getCause() instanceof java.io.IOException, "got " + fail.getCause());
            assertFalse(failing.isCancelled());

            // Control - a successful conversion is unaffected.
            assertEquals("ok", ContinuableFuture.call(() -> "ok").toCompletableFuture(executor).get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
