package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

public class ContinuableFuture100Test extends TestBase {

    private Executor executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(4);
    }

    // Test static factory methods

    @Test
    public void testRun() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            executed.set(true);
        });

        assertNull(future.get());
        assertTrue(executed.get());
    }

    @Test
    public void testRunWithExecutor() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            executed.set(true);
        }, executor);

        assertNull(future.get());
        assertTrue(executed.get());
    }

    @Test
    public void testCall() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "test result");
        assertEquals("test result", future.get());
    }

    @Test
    public void testCallWithExecutor() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "test result", executor);
        assertEquals("test result", future.get());
    }

    @Test
    public void testCompleted() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("completed value");

        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertFalse(future.cancel(true));
        assertEquals("completed value", future.get());
        assertEquals("completed value", future.get(1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWrap() throws Exception {
        Future<String> standardFuture = CompletableFuture.completedFuture("wrapped");
        ContinuableFuture<String> future = ContinuableFuture.wrap(standardFuture);

        assertEquals("wrapped", future.get());
    }

    // Test Future interface methods

    @Test
    public void testCancel() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            latch.await();
            return "should not complete";
        });

        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());
        assertThrows(CancellationException.class, () -> future.get());
        latch.countDown();
    }

    @Test
    public void testIsCancelled() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });

        assertFalse(future.isCancelled());
        future.cancel(true);
        assertTrue(future.isCancelled());
    }

    @Test
    public void testCancelAll() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCall(() -> {
            Thread.sleep(100);
            return "2";
        });

        assertTrue(future2.cancelAll(true));
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
    }

    @Test
    public void testIsAllCancelled() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCall(() -> "2");

        assertFalse(future2.isAllCancelled());
        future2.cancelAll(true);
        assertTrue(future2.isAllCancelled());
    }

    @Test
    public void testIsDone() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "done");
        Thread.sleep(50); // Give it time to complete
        assertTrue(future.isDone());
    }

    @Test
    public void testGet() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "result");
        assertEquals("result", future.get());
    }

    @Test
    public void testGetWithTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "result");
        assertEquals("result", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetWithTimeoutThrowsTimeoutException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "too late";
        });

        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGett() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.gett();

        assertTrue(result.isSuccess());
        assertEquals("success", result.orElseThrow());
    }

    @Test
    public void testGettWithException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        Result<String, Exception> result = future.gett();
        assertTrue(result.isFailure());
        assertNotNull(result.getException());
    }

    @Test
    public void testGettWithTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.gett(1, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertEquals("success", result.orElseThrow());
    }

    @Test
    public void testGetNow() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("done");
        assertEquals("done", future.getNow("default"));

        ContinuableFuture<String> pendingFuture = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "pending";
        });
        assertEquals("default", pendingFuture.getNow("default"));
    }

    @Test
    public void testGetThenApplyWithFunction() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        String result = future.getThenApply(s -> s.toUpperCase());
        assertEquals("TEST", result);
    }

    @Test
    public void testGetThenApplyWithFunctionAndTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        String result = future.getThenApply(1, TimeUnit.SECONDS, s -> s.toUpperCase());
        assertEquals("TEST", result);
    }

    @Test
    public void testGetThenApplyWithBiFunction() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("success");
        String result = future.getThenApply((value, exception) -> {
            return exception == null ? value.toUpperCase() : "error";
        });
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testGetThenApplyWithBiFunctionAndTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("success");
        String result = future.getThenApply(1, TimeUnit.SECONDS, (value, exception) -> {
            return exception == null ? value.toUpperCase() : "error";
        });
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testGetThenAcceptWithConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(s -> ref.set(s.toUpperCase()));
        assertEquals("TEST", ref.get());
    }

    @Test
    public void testGetThenAcceptWithConsumerAndTimeout() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(1, TimeUnit.SECONDS, s -> ref.set(s.toUpperCase()));
        assertEquals("TEST", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept((value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        assertEquals("test", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumerAndTimeout() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(1, TimeUnit.SECONDS, (value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        assertEquals("test", ref.get());
    }

    @Test
    public void testMap() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> mapped = future.map(s -> s.toUpperCase());

        assertEquals("TEST", mapped.get());
    }

    // Test then* methods

    @Test
    public void testThenRunWithRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRun(() -> executed.set(true));
        nextFuture.get();

        assertTrue(executed.get());
    }

    @Test
    public void testThenRunWithConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRun(s -> ref.set(s));
        nextFuture.get();

        assertEquals("test", ref.get());
    }

    @Test
    public void testThenRunWithBiConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRun((value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        nextFuture.get();

        assertEquals("test", ref.get());
    }

    @Test
    public void testThenCallWithCallable() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("first");
        ContinuableFuture<String> nextFuture = future.thenCall(() -> "second");

        assertEquals("second", nextFuture.get());
    }

    @Test
    public void testThenCallWithFunction() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> nextFuture = future.thenCall(s -> s.toUpperCase());

        assertEquals("TEST", nextFuture.get());
    }

    @Test
    public void testThenCallWithBiFunction() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> nextFuture = future.thenCall((value, exception) -> {
            return exception == null ? value.toUpperCase() : "ERROR";
        });

        assertEquals("TEST", nextFuture.get());
    }

    // Test runAfterBoth methods

    @Test
    public void testRunAfterBothWithRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.completed("1");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<Void> combined = future1.runAfterBoth(future2, () -> executed.set(true));
        combined.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterBothWithBiConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("Hello");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("World");

        ContinuableFuture<Void> combined = future1.runAfterBoth(future2, (v1, v2) -> {
            ref.set(v1 + " " + v2);
        });
        combined.get();

        assertEquals("Hello World", ref.get());
    }

    @Test
    public void testRunAfterBothWithTupleConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(42);

        ContinuableFuture<Void> combined = future1.runAfterBoth(future2, tuple -> ref.set(tuple._1 + ":" + tuple._3));
        combined.get();

        assertEquals("success:42", ref.get());
    }

    @Test
    public void testRunAfterBothWithQuadConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(42);

        ContinuableFuture<Void> combined = future1.runAfterBoth(future2, (v1, e1, v2, e2) -> {
            ref.set(v1 + ":" + v2);
        });
        combined.get();

        assertEquals("success:42", ref.get());
    }

    // Test callAfterBoth methods

    @Test
    public void testCallAfterBothWithCallable() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("1");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<String> combined = future1.callAfterBoth(future2, () -> "combined");
        assertEquals("combined", combined.get());
    }

    @Test
    public void testCallAfterBothWithBiFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("Hello");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("World");

        ContinuableFuture<String> combined = future1.callAfterBoth(future2, (v1, v2) -> v1 + " " + v2);
        assertEquals("Hello World", combined.get());
    }

    @Test
    public void testCallAfterBothWithTupleFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("A");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(1);

        ContinuableFuture<String> combined = future1.callAfterBoth(future2, tuple -> tuple._1 + tuple._3);
        assertEquals("A1", combined.get());
    }

    @Test
    public void testCallAfterBothWithQuadFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("A");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(1);

        ContinuableFuture<String> combined = future1.callAfterBoth(future2, (v1, e1, v2, e2) -> v1 + v2);
        assertEquals("A1", combined.get());
    }

    // Test runAfterEither methods

    @Test
    public void testRunAfterEitherWithRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<Void> either = future1.runAfterEither(future2, () -> executed.set(true));
        either.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterEitherWithConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("fast");

        ContinuableFuture<Void> either = future1.runAfterEither(future2, v -> ref.set(v));
        either.get();

        assertEquals("fast", ref.get());
    }

    @Test
    public void testRunAfterEitherWithBiConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<Void> either = future1.runAfterEither(future2, (value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        either.get();

        assertEquals("success", ref.get());
    }

    // Test callAfterEither methods

    @Test
    public void testCallAfterEitherWithCallable() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<String> either = future1.callAfterEither(future2, () -> "either completed");
        assertEquals("either completed", either.get());
    }

    @Test
    public void testCallAfterEitherWithFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("fast");

        ContinuableFuture<String> either = future1.callAfterEither(future2, s -> s.toUpperCase());
        assertEquals("FAST", either.get());
    }

    @Test
    public void testCallAfterEitherWithBiFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<String> either = future1.callAfterEither(future2, (value, exception) -> {
            return exception == null ? value.toUpperCase() : "ERROR";
        });
        assertEquals("SUCCESS", either.get());
    }

    // Test runAfterFirstSucceed methods

    @Test
    public void testRunAfterFirstSucceedWithRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<Void> result = future1.runAfterFirstSucceed(future2, () -> executed.set(true));
        result.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterFirstSucceedWithConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<Void> result = future1.runAfterFirstSucceed(future2, v -> ref.set(v));
        result.get();

        assertEquals("success", ref.get());
    }

    @Test
    public void testRunAfterFirstSucceedWithBiConsumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            N.sleep(20);
            return "second";
        });

        ContinuableFuture<Void> result = future1.runAfterFirstSucceed(future2, (value, exception) -> {
            ref.set(value != null ? value : "error");
        });
        result.get();

        assertEquals("first", ref.get());
    }

    // Test callAfterFirstSucceed methods

    @Test
    public void testCallAfterFirstSucceedWithCallable() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<String> result = future1.callAfterFirstSucceed(future2, () -> "succeeded");
        assertEquals("succeeded", result.get());
    }

    @Test
    public void testCallAfterFirstSucceedWithFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<String> result = future1.callAfterFirstSucceed(future2, s -> s.toUpperCase());
        assertEquals("SUCCESS", result.get());
    }

    @Test
    public void testCallAfterFirstSucceedWithBiFunction() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            N.sleep(20);
            return "second";
        });

        ContinuableFuture<String> result = future1.callAfterFirstSucceed(future2, (value, exception) -> {
            return value != null ? value.toUpperCase() : "ERROR";
        });
        assertEquals("FIRST", result.get());
    }

    // Test thenDelay

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay() throws Exception {
        long startTime = System.currentTimeMillis();
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 80); // Allow some tolerance
    }

    @Test
    public void testThenDelayWithZeroDelay() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(0, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
    }

    @Test
    public void testThenDelayWithNegativeDelay() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(-100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
    }

    // Test thenUse

    @Test
    public void testThenUse() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenUse(customExecutor).thenCall(() -> {
            threadRef.set(Thread.currentThread());
            return "executed";
        });

        assertEquals("executed", future.get());
        assertNotNull(threadRef.get());
    }

    // Test edge cases and error handling

    @Test
    public void testWithNullExecutor() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        assertThrows(IllegalArgumentException.class, () -> future.thenUse(null));
    }

    @Test
    public void testExecutionException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test exception");
        });

        assertThrows(ExecutionException.class, () -> future.get());
    }

    @Test
    public void testInterruptedException() throws Exception {
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
                // ignore
            }
        });
        interrupter.start();

        assertThrows(InterruptedException.class, () -> future.get());
        latch.countDown();
    }

    @Test
    public void testChainedOperations() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            counter.incrementAndGet();
            return "1";
        }).thenCall(s -> {
            counter.incrementAndGet();
            return s + "2";
        }).thenDelay(50, TimeUnit.MILLISECONDS).thenCall(s -> {
            counter.incrementAndGet();
            return s + "3";
        });

        assertEquals("123", future.get());
        assertEquals(3, counter.get());
    }

    @Test
    public void testBothFuturesFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<Void> result = future1.runAfterFirstSucceed(future2, () -> {
            // Should not execute
        });

        assertThrows(ExecutionException.class, () -> result.get());
    }

    @Test
    public void testWithDelay() throws Exception {
        long startTime = System.currentTimeMillis();
        ContinuableFuture<String> future = ContinuableFuture.completed("test").with(executor, 100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 90); // Allow some tolerance
    }

    @Test
    public void testMapWithException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        ContinuableFuture<String> mapped = future.map(s -> {
            throw new IllegalArgumentException("mapping error");
        });

        assertThrows(IllegalArgumentException.class, () -> mapped.get());
    }

    @Test
    public void testCancelAllWithMultipleUpstreams() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "2";
        });

        ContinuableFuture<String> combined = future1.callAfterBoth(future2, (v1, v2) -> v1 + v2);

        assertTrue(combined.cancelAll(true));
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());
        assertTrue(combined.isCancelled());
    }

    @Test
    public void testIsAllCancelledWithPartialCancellation() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "1";
        });

        ContinuableFuture<String> future2 = future1.thenCall(s -> s + "2");

        // Only cancel future2, not future1
        future2.cancel(true);

        assertFalse(future2.isAllCancelled()); // Because future1 is not cancelled
    }

    @Test
    public void testRunAfterBothWithOneFailed() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            throw new RuntimeException("failure");
        });

        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<Void> combined = future1.runAfterBoth(future2, (v1, e1, v2, e2) -> {
            ref.set((e1 == null ? v1 : "error1") + ":" + (e2 == null ? v2 : "error2"));
        });

        combined.get();
        assertEquals("success:error2", ref.get());
    }

    @Test
    public void testCallAfterEitherWithBothFailed() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<String> either = future1.callAfterEither(future2, () -> "either completed");

        assertThrows(ExecutionException.class, () -> either.get());
    }

    @Test
    public void testGettWithCancellation() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "should be cancelled";
        });

        future.cancel(true);
        Result<String, Exception> result = future.gett();

        assertTrue(result.isFailure());
        assertTrue(result.getException() instanceof CancellationException);
    }

    @Test
    public void testGetNowWithException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("immediate failure");
        });

        Thread.sleep(50); // Ensure the future has completed
        assertThrows(ExecutionException.class, () -> future.getNow("default"));
    }

    @Test
    public void testComplexChainWithErrors() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();

        ContinuableFuture<Void> future = ContinuableFuture.call(() -> "start").thenCall(s -> {
            if (s.equals("start")) {
                throw new RuntimeException("intentional error");
            }
            return s;
        }).thenRun((value, exception) -> {
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
    public void testRunAfterFirstSucceedAllFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<Void> result = future1.runAfterFirstSucceed(future2, () -> {
            // Should not execute
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> result.get());
        assertTrue(ex.getCause().getMessage().contains("fail1"));
    }

    @Test
    public void testCallAfterFirstSucceedSecondSucceeds() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "success2";
        });

        ContinuableFuture<String> result = future1.callAfterFirstSucceed(future2, s -> s.toUpperCase());
        assertEquals("SUCCESS2", result.get());
    }

    @Test
    public void testThenDelayIsDone() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(100, TimeUnit.MILLISECONDS);

        assertTrue(future.isDone());
        Thread.sleep(150);
        assertTrue(future.isDone());
        assertEquals("test", future.get());
    }

    @Test
    public void testNestedFutures() throws Exception {
        ContinuableFuture<String> outer = ContinuableFuture.call(() -> {
            ContinuableFuture<String> inner = ContinuableFuture.call(() -> "inner");
            return inner.get() + "-outer";
        });

        assertEquals("inner-outer", outer.get());
    }

    @Test
    public void testTimeoutInChain() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "slow";
        }).thenCall(s -> s + "-processed");

        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
    }
}
