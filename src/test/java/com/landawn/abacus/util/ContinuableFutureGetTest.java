package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class ContinuableFutureGetTest extends ContinuableFutureTestSupport {
    @Test
    public void testGetAsResult_restoresInterruptStatus() {
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> future = ContinuableFuture.wrap(pending);

        try {
            Thread.currentThread().interrupt();
            final Result<String, Exception> untimedResult = future.getAsResult();

            assertTrue(untimedResult.getException() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());

            Thread.interrupted();
            Thread.currentThread().interrupt();
            final Result<String, Exception> timedResult = future.getAsResult(1, TimeUnit.SECONDS);

            assertTrue(timedResult.getException() instanceof InterruptedException);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            pending.cancel(true);
        }
    }

    @Test
    public void testGet_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "result");
        assertEquals("result", future.get());
    }

    @Test
    public void testGet_throwsExecutionException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertEquals("error", ex.getCause().getMessage());
    }

    @Test
    public void testGet_throwsCancellationException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });
        future.cancel(true);

        assertThrows(CancellationException.class, () -> future.get());
    }

    @Test
    public void testGetWithTimeout_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "result");
        assertEquals("result", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetWithTimeout_throwsTimeoutException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "too late";
        });

        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
        future.cancel(true);
    }

    @Test
    public void testGetWithTimeout_throwsExecutionException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalArgumentException("invalid");
        });

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void testGetNow_completed() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("done");
        assertEquals("done", future.getNow("default"));
    }

    @Test
    public void testGetNow_pending() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "pending";
        });
        assertEquals("default", future.getNow("default"));
        future.cancel(true);
    }

    @Test
    public void testGetNow_withNullDefault() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "pending";
        });
        assertNull(future.getNow(null));
        future.cancel(true);
    }

    @Test
    public void testGetNow_throwsExecutionException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("immediate failure");
        });

        Thread.sleep(50);
        assertThrows(ExecutionException.class, () -> future.getNow("default"));
    }

    @Test
    public void testGetNow_withCancelled() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "test";
        });
        future.cancel(true);

        assertThrows(CancellationException.class, () -> future.getNow("default"));
    }

    @Test
    public void testGetThenApplyWithFunction_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        String result = future.getThenApply(s -> s.toUpperCase());
        assertEquals("TEST", result);
    }

    @Test
    public void testGetThenApplyWithFunction_null() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        String result = future.getThenApply(s -> s == null ? "NULL" : s);
        assertEquals("NULL", result);
    }

    @Test
    public void testGetThenApplyWithFunction_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });

        assertThrows(ExecutionException.class, () -> future.getThenApply(s -> s.toUpperCase()));
    }

    @Test
    public void testGetThenApplyWithFunctionAndTimeout_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        String result = future.getThenApply(1, TimeUnit.SECONDS, s -> s.toUpperCase());
        assertEquals("TEST", result);
    }

    @Test
    public void testGetThenApplyWithFunctionAndTimeout_timeout() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "slow";
        });

        assertThrows(TimeoutException.class, () -> future.getThenApply(100, TimeUnit.MILLISECONDS, s -> s.toUpperCase()));
        future.cancel(true);
    }

    @Test
    public void testGetThenApplyWithBiFunction_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("success");
        String result = future.getThenApply((value, exception) -> {
            return exception == null ? value.toUpperCase() : "error";
        });
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testGetThenApplyWithBiFunction_handlesException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        String result = future.getThenApply((value, exception) -> {
            return exception != null ? "ERROR_HANDLED" : value;
        });
        assertEquals("ERROR_HANDLED", result);
    }

    @Test
    public void testGetThenApplyWithBiFunction_nullValue() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        String result = future.getThenApply((value, exception) -> {
            return exception == null ? "NULL_VALUE" : "ERROR";
        });
        assertEquals("NULL_VALUE", result);
    }

    @Test
    public void testGetThenApplyWithBiFunctionAndTimeout_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("success");
        String result = future.getThenApply(1, TimeUnit.SECONDS, (value, exception) -> {
            return exception == null ? value.toUpperCase() : "error";
        });
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testGetThenApplyWithBiFunctionAndTimeout_handlesException() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalArgumentException("invalid arg");
        });

        String result = future.getThenApply(1, TimeUnit.SECONDS, (value, exception) -> {
            return exception != null ? "HANDLED" : value;
        });
        assertEquals("HANDLED", result);
    }

    @Test
    public void testGetThenApplyWithBiFunctionAndTimeout_timeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "slow";
        });

        String result = future.getThenApply(100, TimeUnit.MILLISECONDS, (value, exception) -> {
            return exception != null ? "TIMEOUT" : value;
        });
        assertEquals("TIMEOUT", result);
        future.cancel(true);
    }

    @Test
    public void testGetThenAcceptWithConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(s -> ref.set(s.toUpperCase()));
        assertEquals("TEST", ref.get());
    }

    @Test
    public void testGetThenAcceptWithConsumer_null() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>("initial");
        ContinuableFuture<String> future = ContinuableFuture.completed(null);

        future.getThenAccept(s -> ref.set(s == null ? "NULL" : s));
        assertEquals("NULL", ref.get());
    }

    @Test
    public void testGetThenAcceptWithConsumer_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });

        assertThrows(ExecutionException.class, () -> future.getThenAccept(s -> {
        }));
    }

    @Test
    public void testGetThenAcceptWithConsumerAndTimeout_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(1, TimeUnit.SECONDS, s -> ref.set(s.toUpperCase()));
        assertEquals("TEST", ref.get());
    }

    @Test
    public void testGetThenAcceptWithConsumerAndTimeout_timeout() {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "slow";
        });

        assertThrows(TimeoutException.class, () -> future.getThenAccept(100, TimeUnit.MILLISECONDS, s -> ref.set(s)));
        future.cancel(true);
    }

    @Test
    public void testGetThenAcceptWithBiConsumer_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept((value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        assertEquals("test", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumer_handlesException() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("test error");
        });

        future.getThenAccept((value, exception) -> {
            ref.set(exception != null ? "ERROR_HANDLED" : value);
        });
        assertEquals("ERROR_HANDLED", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumer_nullValue() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed(null);

        future.getThenAccept((value, exception) -> {
            ref.set(exception == null && value == null ? "NULL" : "NOT_NULL");
        });
        assertEquals("NULL", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumerAndTimeout_success() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        future.getThenAccept(1, TimeUnit.SECONDS, (value, exception) -> {
            ref.set(exception == null ? value : "error");
        });
        assertEquals("test", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumerAndTimeout_handlesException() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalStateException("state error");
        });

        future.getThenAccept(1, TimeUnit.SECONDS, (value, exception) -> {
            ref.set(exception != null ? "HANDLED" : value);
        });
        assertEquals("HANDLED", ref.get());
    }

    @Test
    public void testGetThenAcceptWithBiConsumerAndTimeout_timeout() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "slow";
        });

        future.getThenAccept(100, TimeUnit.MILLISECONDS, (value, exception) -> {
            ref.set(exception != null ? "TIMEOUT" : value);
        });
        assertEquals("TIMEOUT", ref.get());
        future.cancel(true);
    }

    @Test
    public void testGetThenApplyAndGetThenAccept_cancelled() {
        // Contract pin for the four single-action overloads: each one calls get()/get(timeout, unit) directly, so a
        // cancelled computation surfaces as CancellationException and the action is never invoked. The four
        // BiFunction/BiConsumer overloads deliberately differ - they route through getAsResult().
        final AtomicReference<String> seen = new AtomicReference<>();

        assertThrows(CancellationException.class, () -> cancelledStringFuture().getThenApply(s -> seen.getAndSet(s)));
        assertThrows(CancellationException.class, () -> cancelledStringFuture().getThenApply(1, TimeUnit.SECONDS, s -> seen.getAndSet(s)));
        assertThrows(CancellationException.class, () -> cancelledStringFuture().getThenAccept(s -> seen.set(s)));
        assertThrows(CancellationException.class, () -> cancelledStringFuture().getThenAccept(1, TimeUnit.SECONDS, s -> seen.set(s)));

        assertNull(seen.get());
    }

    private static ContinuableFuture<String> cancelledStringFuture() {
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        assertTrue(cancelled.cancel(false));

        return ContinuableFuture.wrap(cancelled);
    }
}
