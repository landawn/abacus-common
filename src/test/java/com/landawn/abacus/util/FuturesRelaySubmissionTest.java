package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
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
        final long originalKeepAliveNanos = executor.getKeepAliveTime(TimeUnit.NANOSECONDS);
        // Relay threads left idle by earlier tests stay alive for the pool's 60 s keep-alive and would keep the pool
        // above the one-thread ceiling this test needs; let them retire quickly while it runs (shortening the
        // keep-alive interrupts idle workers, which re-poll with the new timeout and exit). A relay still PARKED
        // in a pending input's get() cannot be retired this way - the wait loop below names it if that ever happens.
        executor.setKeepAliveTime(20, TimeUnit.MILLISECONDS);

        try {
            runBothSubmissionFailures(executor, originalMaximum, originalHandler);
        } finally {
            executor.setKeepAliveTime(originalKeepAliveNanos, TimeUnit.NANOSECONDS);
        }
    }

    private static void runBothSubmissionFailures(final ThreadPoolExecutor executor, final int originalMaximum, final RejectedExecutionHandler originalHandler)
            throws Exception {
        // Exercise both branches of the submission catch without exhausting machine resources or
        // shutting down the process-wide executor. Always restore its configuration afterwards.
        for (final Throwable failure : List.of(new RejectedExecutionException("relay rejected"), new AssertionError("relay submission failed"))) {
            final CountDownLatch entered = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final CountDownLatch exited = new CountDownLatch(1);
            try {
                // Wait until the pool is EMPTY (the short keep-alive retires idle relays within a few ms), then let
                // the blocker create the pool's only thread and cap the pool at that one thread. Capping first and
                // reusing an idle thread does not work: an idle worker that is just timing out still counts in the
                // pool size but no longer polls the hand-off queue, so the blocker itself would be rejected.
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while ((executor.getPoolSize() > 0 || executor.getActiveCount() > 0) && System.nanoTime() < deadline) {
                    Thread.sleep(5);
                }
                assertEquals(0, executor.getActiveCount(), "a relay is still parked in a pending input's get() - an earlier test left it behind");
                assertEquals(0, executor.getPoolSize(), "relay pool still holds " + executor.getPoolSize() + " idle threads");
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
                executor.setMaximumPoolSize(1);
                executor.setRejectedExecutionHandler((task, pool) -> {
                    if (failure instanceof Error error) {
                        throw error;
                    }
                    throw (RuntimeException) failure;
                });

                // A done FutureTask SUBCLASS is observed through a relay (a completed plain task or
                // ContinuableFuture.completed(..) would be read inline and never touch the relay pool).
                final FutureTask<String> input = new FutureTask<>(() -> "winner") {
                };
                input.run();
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
