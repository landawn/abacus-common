package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ContinuableFutureCallTest extends ContinuableFutureTestSupport {
    @Test
    public void testCall_basic() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "test result");
        assertEquals("test result", future.get());
    }

    @Test
    public void testCall_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> null);
        assertNull(future.get());
    }

    @Test
    public void testCall_withException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("call error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("call error", ex.getCause().getMessage());
    }

    @Test
    public void testCallAfterBothWithCallable_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("1");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, () -> "combined");
        assertEquals("combined", combined.get());
    }

    @Test
    public void testCallAfterBothWithCallable_waitForBoth() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "2";
        });

        long start = System.currentTimeMillis();
        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, () -> "done");
        assertEquals("done", combined.get());
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 80);
    }

    @Test
    public void testCallAfterBothWithBiFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("Hello");
        ContinuableFuture<String> future2 = ContinuableFuture.completed("World");

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, v2) -> v1 + " " + v2);
        assertEquals("Hello World", combined.get());
    }

    @Test
    @Timeout(3)
    public void testCallAfterBothWithBiFunction_waitsForOtherAfterFirstFailure() throws Exception {
        final ContinuableFuture<String> failed = ContinuableFuture.call(() -> {
            throw new IllegalArgumentException("first failed");
        });
        assertThrows(ExecutionException.class, failed::get);

        final CountDownLatch releaseSecond = new CountDownLatch(1);
        final ContinuableFuture<String> second = ContinuableFuture.call(() -> {
            releaseSecond.await();
            return "second";
        });
        final AtomicBoolean functionExecuted = new AtomicBoolean();
        final ContinuableFuture<String> combined = failed.callAsyncAfterBoth(second, (firstValue, secondValue) -> {
            functionExecuted.set(true);
            return firstValue + secondValue;
        });

        try {
            assertThrows(TimeoutException.class, () -> combined.get(50, TimeUnit.MILLISECONDS));
        } finally {
            releaseSecond.countDown();
        }

        final ExecutionException exception = assertThrows(ExecutionException.class, combined::get);
        assertEquals("first failed", exception.getCause().getMessage());
        assertFalse(functionExecuted.get());
    }

    @Test
    public void testCallAfterBothWithBiFunction_differentTypes() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("Count:");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(42);

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, v2) -> v1 + v2);
        assertEquals("Count:42", combined.get());
    }

    @Test
    public void testCallAfterBothWithTupleFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("A");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(1);

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, tuple -> tuple._1 + tuple._3);
        assertEquals("A1", combined.get());
    }

    @Test
    public void testCallAfterBothWithQuadFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("A");
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(1);

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, e1, v2, e2) -> v1 + v2);
        assertEquals("A1", combined.get());
    }

    @Test
    public void testCallAfterBothWithQuadFunction_handlesExceptions() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("error1");
        });
        ContinuableFuture<Integer> future2 = ContinuableFuture.completed(1);

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, e1, v2, e2) -> {
            return (e1 != null ? "ERROR" : v1) + ":" + v2;
        });
        assertEquals("ERROR:1", combined.get());
    }

    @Test
    public void testCallAfterEitherWithCallable_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "1";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("2");

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, () -> "either completed");
        assertEquals("either completed", either.get());
        future1.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithCallable_firstCompletes() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("fast");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, () -> "done");
        assertEquals("done", either.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("fast");

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, s -> s.toUpperCase());
        assertEquals("FAST", either.get());
        future1.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithFunction_transformsValue() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("test");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "delayed";
        });

        ContinuableFuture<Integer> either = future1.callAsyncAfterEither(future2, String::length);
        assertEquals(4, either.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithBiFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("success");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, (value, exception) -> {
            return exception == null ? value.toUpperCase() : "ERROR";
        });
        assertEquals("SUCCESS", either.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithBiFunction_handlesException() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, (value, exception) -> {
            return exception != null ? "RECOVERED" : value;
        });
        assertEquals("RECOVERED", either.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterEitherWithBiFunction_bothFail() throws InterruptedException, ExecutionException {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<String> either = future1.callAsyncAfterEither(future2, () -> "either completed");

        assertEquals("either completed", either.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterFirstSuccessWithCallable_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, () -> "succeeded");
        assertEquals("succeeded", result.get());
    }

    @Test
    public void testCallAfterFirstSuccessWithCallable_firstSucceeds() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            throw new RuntimeException("fail");
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, () -> "done");
        assertEquals("done", result.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterFirstSuccessWithCallable_bothFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, () -> "should not execute");

        ExecutionException ex = assertThrows(ExecutionException.class, () -> result.get());
        assertNotNull(ex.getCause());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterFirstSuccessWithFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.completed("success");

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, s -> s.toUpperCase());
        assertEquals("SUCCESS", result.get());
    }

    @Test
    public void testCallAfterFirstSuccessWithFunction_secondSucceeds() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            return "success2";
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, s -> s.toUpperCase());
        assertEquals("SUCCESS2", result.get());
    }

    @Test
    public void testCallAfterFirstSuccessWithBiFunction_success() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed("first");
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            N.sleep(100);
            return "second";
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, (value, exception) -> {
            return value != null ? value.toUpperCase() : "ERROR";
        });
        assertEquals("FIRST", result.get());
        future2.cancel(true);
    }

    @Test
    public void testCallAfterFirstSuccessWithBiFunction_secondSucceeds() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "second";
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, (value, exception) -> {
            return exception == null ? value.toUpperCase() : "ERROR";
        });
        assertEquals("SECOND", result.get());
    }

    @Test
    public void testCall_withExecutor() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "custom executor result", customExecutor);

        assertEquals("custom executor result", future.get());
    }

    @Test
    public void testCall_withExecutor_exception() {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalStateException("executor call error");
        }, customExecutor);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    @Test
    public void testCall_withExecutor_null() throws Exception {
        Executor customExecutor = Executors.newSingleThreadExecutor();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> null, customExecutor);

        assertNull(future.get());
    }

    @Test
    public void testCallAfterBothWithBiFunction_withNull() throws Exception {
        ContinuableFuture<String> future1 = ContinuableFuture.completed(null);
        ContinuableFuture<String> future2 = ContinuableFuture.completed("value");

        ContinuableFuture<String> combined = future1.callAsyncAfterBoth(future2, (v1, v2) -> (v1 == null ? "NULL" : v1) + ":" + v2);
        assertEquals("NULL:value", combined.get());
    }

    @Test
    public void testCallAfterFirstSuccessWithFunction_bothFail() {
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("fail1");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(50);
            throw new RuntimeException("fail2");
        });

        ContinuableFuture<String> result = future1.callAsyncAfterFirstSuccess(future2, s -> s.toUpperCase());

        assertThrows(ExecutionException.class, () -> result.get());
        future2.cancel(true);
    }
}
