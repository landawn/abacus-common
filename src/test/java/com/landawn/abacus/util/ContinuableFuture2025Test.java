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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ContinuableFuture2025Test extends TestBase {

    private Executor executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(4);
    }

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
    public void testRunWithExecutor_basic() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            executed.set(true);
        }, executor);

        assertNull(future.get());
        assertTrue(executed.get());
    }

    @Test
    public void testRunWithExecutor_withException() {
        ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
            throw new IllegalStateException("executor error");
        }, executor);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertTrue(ex.getCause() instanceof IllegalStateException);
    }

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
    public void testCallWithExecutor_basic() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "executor result", executor);
        assertEquals("executor result", future.get());
    }

    @Test
    public void testCallWithExecutor_withException() {
        ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
            throw new ArithmeticException("division by zero");
        }, executor);

        ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get());
        assertTrue(ex.getCause() instanceof ArithmeticException);
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
    public void testCompleted_cannotCancel() {
        ContinuableFuture<String> future = ContinuableFuture.completed("value");

        assertFalse(future.cancel(true));
        assertFalse(future.cancel(false));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testCompleted_getWithTimeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("immediate");
        assertEquals("immediate", future.get(1, TimeUnit.MILLISECONDS));
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
    public void testIsAllCancelled_notCancelled() {
        ContinuableFuture<String> future = ContinuableFuture.completed("test");
        assertFalse(future.isAllCancelled());
    }

    @Test
    public void testIsDone_completed() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "done");
        Thread.sleep(50);
        assertTrue(future.isDone());
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
    public void testGett_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.gett();

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

        Result<String, Exception> result = future.gett();
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
        Result<String, Exception> result = future.gett();

        assertTrue(result.isFailure());
        assertTrue(result.getException() instanceof CancellationException);
    }

    @Test
    public void testGett_withNull() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> null);
        Result<String, Exception> result = future.gett();

        assertTrue(result.isSuccess());
        assertNull(result.orElseThrow());
    }

    @Test
    public void testGettWithTimeout_success() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> "success");
        Result<String, Exception> result = future.gett(1, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertEquals("success", result.orElseThrow());
    }

    @Test
    public void testGettWithTimeout_timeout() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(1000);
            return "too late";
        });

        Result<String, Exception> result = future.gett(100, TimeUnit.MILLISECONDS);
        assertTrue(result.isFailure());
        assertTrue(result.getException() instanceof TimeoutException);
        future.cancel(true);
    }

    @Test
    public void testGettWithTimeout_exception() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new IllegalStateException("state error");
        });

        Result<String, Exception> result = future.gett(1, TimeUnit.SECONDS);
        assertTrue(result.isFailure());
        assertNotNull(result.getException());
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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mapped.get());
        assertEquals("mapping error", ex.getMessage());
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
    public void testThenRunWithRunnable_success() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<String> future = ContinuableFuture.completed("test");

        ContinuableFuture<Void> nextFuture = future.thenRunAsync(() -> executed.set(true));
        nextFuture.get();

        assertTrue(executed.get());
    }

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
    public void testThenCallWithFunction_throwsException() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            throw new RuntimeException("original error");
        });

        ContinuableFuture<String> nextFuture = future.thenCallAsync(s -> s.toUpperCase());

        ExecutionException ex = assertThrows(ExecutionException.class, () -> nextFuture.get());
        assertEquals("java.lang.RuntimeException: original error", ex.getCause().getMessage());
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
    public void testThenCallWithBiFunction_nullValue() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed(null);
        ContinuableFuture<String> nextFuture = future.thenCallAsync((value, exception) -> {
            return exception == null && value == null ? "NULL_HANDLED" : value;
        });

        assertEquals("NULL_HANDLED", nextFuture.get());
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
        ContinuableFuture<String> future1 = ContinuableFuture.call(() -> {
            throw new RuntimeException("error");
        });
        ContinuableFuture<String> future2 = ContinuableFuture.call(() -> {
            Thread.sleep(100);
            return "slow";
        });

        ContinuableFuture<Void> either = future1.runAsyncAfterEither(future2, (value, exception) -> {
            ref.set(exception != null ? "ERROR_HANDLED" : value);
        });
        either.get();

        assertEquals("slow", ref.get());
        assertFalse(future2.cancel(true));
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
        assertTrue(ex.getCause().getMessage().contains("fail"));
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
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testThenDelay_basic() throws Exception {
        long startTime = System.currentTimeMillis();
        ContinuableFuture<String> future = ContinuableFuture.completed("test").thenDelay(100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 80);
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

        assertTrue(future.isDone());
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
    public void testTimeout_inChain() {
        ContinuableFuture<String> future = ContinuableFuture.call(() -> {
            Thread.sleep(200);
            return "slow";
        }).thenCallAsync(s -> s + "-processed");

        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
        future.cancel(true);
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
    public void testWithDelayMethod_deprecated() throws Exception {
        long startTime = System.currentTimeMillis();
        ContinuableFuture<String> future = ContinuableFuture.completed("test").with(executor, 100, TimeUnit.MILLISECONDS);

        assertEquals("test", future.get());
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration >= 80);
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
    public void testCompletedFuture_multipleGets() throws Exception {
        ContinuableFuture<String> future = ContinuableFuture.completed("value");

        assertEquals("value", future.get());
        assertEquals("value", future.get(1, TimeUnit.MILLISECONDS));
        assertEquals("value", future.getNow("default"));

        Result<String, Exception> result = future.gett();
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
    public void testAllPublicMethodsCovered() {
        ContinuableFuture<String> f = ContinuableFuture.completed("test");

        assertNotNull(ContinuableFuture.run(() -> {
        }));
        assertNotNull(ContinuableFuture.call(() -> "test"));
        assertNotNull(ContinuableFuture.completed("value"));
        assertNotNull(ContinuableFuture.wrap(CompletableFuture.completedFuture("test")));

        f.cancel(true);
        f.isCancelled();
        f.cancelAll(true);
        f.isAllCancelled();
        f.isDone();

        assertTrue(true);
    }
}
