package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
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

public class ContinuableFutureRunTest extends ContinuableFutureTestSupport {
    @Test
    public void testRun_basic() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            executed.set(true);
        });

        assertNull(future.get());
        assertTrue(executed.get());
    }

    @Test
    public void testRun_withException() {
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            throw new RuntimeException("test error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("test error", ex.getCause().getMessage());
    }

    @Test
    public void testRunAfterBothWithRunnable_success() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.completed("1");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, () -> executed.set(true));
        combined.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterBothWithRunnable_waitForBoth() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "2";
        });

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, () -> executed.set(true));
        combined.get();

        assertTrue(executed.get());
        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
    }

    @Test
    @Timeout(3)
    public void testRunAfterBothWithRunnable_waitsForOtherAfterFirstFailure() throws Exception {
        final ContinuableFuture<String> failed = ContinuableFuture.call(() -> {
            throw new IllegalStateException("first failed");
        });
        assertThrows(ExecutionException.class, failed::get); // make the first failure deterministic

        final CountDownLatch secondStarted = new CountDownLatch(1);
        final CountDownLatch releaseSecond = new CountDownLatch(1);
        final AtomicBoolean actionExecuted = new AtomicBoolean();
        final ContinuableFuture<String> second = ContinuableFuture.call(() -> {
            secondStarted.countDown();
            releaseSecond.await();
            return "second";
        });
        final ContinuableFuture<Void> combined = failed.runAsyncAfterBoth(second, () -> actionExecuted.set(true));

        assertTrue(secondStarted.await(1, TimeUnit.SECONDS));

        try {
            assertThrows(TimeoutException.class, () -> combined.get(50, TimeUnit.MILLISECONDS));
        } finally {
            releaseSecond.countDown();
        }

        final ExecutionException exception = assertThrows(ExecutionException.class, combined::get);
        assertEquals("first failed", exception.getCause().getMessage());
        assertFalse(actionExecuted.get());
    }

    @Test
    public void testRunAfterBothWithBiConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("Hello");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("World");

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, (v1, v2) -> {
            ref.set(v1 + " " + v2);
        });
        combined.get();

        assertEquals("Hello World", ref.get());
    }

    @Test
    public void testRunAfterBothWithBiConsumer_withNull() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed(null);
        ContinuableFuture<String> future2 = ContinuableFuture.completed("value");

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, (v1, v2) -> {
            ref.set((v1 == null ? "NULL" : v1) + ":" + v2);
        });
        combined.get();

        assertEquals("NULL:value", ref.get());
    }

    @Test
    public void testRunAfterBothWithTupleConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(42);

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, tuple -> {
            ref.set(tuple._1 + ":" + tuple._3);
        });
        combined.get();

        assertEquals("success:42", ref.get());
    }

    @Test
    public void testRunAfterBothWithQuadConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(42);

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, (v1, e1, v2, e2) -> {
            ref.set(v1 + ":" + v2);
        });
        combined.get();

        assertEquals("success:42", ref.get());
    }

    @Test
    public void testRunAfterBothWithQuadConsumer_handlesExceptions() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            throw new RuntimeException("failure");
        });

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, (v1, e1, v2, e2) -> {
            ref.set((e1 == null ? v1 : "error1") + ":" + (e2 == null ? v2 : "error2"));
        });

        combined.get();
        assertEquals("success:error2", ref.get());
    }

    @Test
    public void testRunAfterEitherWithRunnable_success() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, () -> executed.set(true));
        either.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterEitherWithRunnable_firstCompletes() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.completed("fast");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, () -> executed.set(true));
        either.get();

        assertTrue(executed.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterEitherWithConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("fast");

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, v -> ref.set(v));
        either.get();

        assertEquals("fast", ref.get());
        future1.cancel(true);
    }

    @Test
    public void testRunAfterEitherWithConsumer_getsFirstValue() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "second";
        });

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, v -> ref.set(v));
        either.get();

        assertEquals("first", ref.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterEitherWithBiConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, (value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        either.get();

        assertEquals("success", ref.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterEitherWithBiConsumer_handlesException() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("error"));
        ContinuableFuture<String> future1 = new ContinuableFuture<>(failed);
        ContinuableFuture<String> future2 = new ContinuableFuture<>(new CompletableFuture<>());

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, (value, exception) -> {
            ref.set(exception != null ? "ERROR_HANDLED" : value);
        });
        try {
            either.get(1, TimeUnit.SECONDS);
            assertEquals("ERROR_HANDLED", ref.get());
            assertFalse(future2.isDone());
        } finally {
            assertTrue(future2.cancel(true));
        }
    }

    @Test
    public void testRunAfterFirstSuccessWithRunnable_success() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, () -> executed.set(true));
        result.get();

        assertTrue(executed.get());
    }

    @Test
    public void testRunAfterFirstSuccessWithRunnable_firstSucceeds() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "second";
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, () -> executed.set(true));
        result.get();

        assertTrue(executed.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterFirstSuccessWithRunnable_bothFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, () -> {
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> result.get());
        assertTrue(ex.getCause().getMessage().contains("fail1"));
        future2.cancel(true);
    }

    @Test
    public void testRunAfterFirstSuccessWithConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, v -> ref.set(v));
        result.get();

        assertEquals("success", ref.get());
    }

    @Test
    public void testRunAfterFirstSuccessWithConsumer_firstSucceeds() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            N.sleep(50);
            throw new RuntimeException("fail");
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, v -> ref.set(v));
        result.get();

        assertEquals("first", ref.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterFirstSuccessWithBiConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            N.sleep(20);
            return "second";
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, (value, exception) -> {
            ref.set(value != null ? value : "error");
        });
        result.get();

        assertEquals("first", ref.get());
        future2.cancel(true);
    }

    @Test
    public void testRunAfterFirstSuccessWithBiConsumer_secondSucceeds() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "second";
        });

        ContinuableFuture<Void> result = future1.runAsyncAfterFirstSuccess(future2, (value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        result.get();

        assertEquals("second", ref.get());
    }

    @Test
    public void testRun_withExecutor() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<Thread> threadRef = new AtomicReference<>();

        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            executed.set(true);
            threadRef.set(Thread.currentThread());
        }, customExecutor);

        assertNull(future.get());
        assertTrue(executed.get());
        assertNotNull(threadRef.get());
    }

    @Test
    public void testRun_withExecutor_exception() {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            throw new RuntimeException("executor error");
        }, customExecutor);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("executor error", ex.getCause().getMessage());
    }

    @Test
    public void testRunAfterBothWithBiConsumer_bothException() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<Void> combined = future1.runAsyncAfterBoth(future2, (v1, e1, v2, e2) -> {
            ref.set((e1 != null ? "error1" : v1) + ":" + (e2 != null ? "error2" : v2));
        });
        combined.get();

        assertEquals("error1:error2", ref.get());
    }

    @Test
    public void testRunAfterEitherWithRunnable_bothCompleted() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future1 = ContinuableFuture.completed("A");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("B");

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, () -> executed.set(true));
        either.get();

        assertTrue(executed.get());
    }
}
