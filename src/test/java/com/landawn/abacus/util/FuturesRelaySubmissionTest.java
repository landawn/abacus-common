package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;

import com.landawn.abacus.TestBase;

/** Temporarily saturates the shared relay pool; no other test may use Futures during this test. */
@Tag("unit")
@Isolated
class FuturesRelaySubmissionTest extends TestBase {
    @Test
    @Timeout(20)
    void rejectedRelaySubmissionPropagatesAndAllowsRetry() throws Exception {
        final Field field = Futures.class.getDeclaredField("RELAY_EXECUTOR");
        field.setAccessible(true);
        final ThreadPoolExecutor executor = (ThreadPoolExecutor) field.get(null);
        final int originalMaximum = executor.getMaximumPoolSize();
        final RejectedExecutionHandler originalHandler = executor.getRejectedExecutionHandler();

        // Exercise both branches of the submission catch without exhausting machine resources or
        // shutting down the process-wide executor. Always restore its configuration afterwards.
        for (final Throwable failure : List.of(new RejectedExecutionException("relay rejected"), new AssertionError("relay submission failed"))) {
            final CountDownLatch entered = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch exited = new CountDownLatch(1);
            try {
                executor.setMaximumPoolSize(1);
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while ((executor.getPoolSize() > 1 || executor.getActiveCount() > 0) && System.nanoTime() < deadline) {
                    Thread.sleep(5);
                }
                assertTrue(executor.getPoolSize() <= 1);
                assertEquals(0, executor.getActiveCount());
                executor.execute(() -> {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exited.countDown();
                    }
                });
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                executor.setRejectedExecutionHandler((task, pool) -> {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    throw (RuntimeException) failure;
                });

                final ContinuableFuture<String> input = ContinuableFuture.completed("winner");
                final ContinuableFuture<String> any = Futures.anyOf(input);
                assertSame(failure, assertThrows(failure.getClass(), () -> any.get(1, TimeUnit.SECONDS)));
                assertFalse(input.isCancelled());

                executor.setRejectedExecutionHandler(originalHandler);
                executor.setMaximumPoolSize(originalMaximum);
                assertEquals("winner", any.get(2, TimeUnit.SECONDS));
                assertEquals("winner", any.get());
            } finally {
                executor.setRejectedExecutionHandler(originalHandler);
                executor.setMaximumPoolSize(originalMaximum);
                release.countDown();
                if (entered.getCount() == 0) {
                    assertTrue(exited.await(2, TimeUnit.SECONDS));
                }
            }
        }
    }
}
