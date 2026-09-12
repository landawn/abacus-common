package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.TestBase;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class DelayCancellationTest extends TestBase {

    @Test
    void directAndPreexistingCancellationBypassTheDelay() {
        for (final boolean beforeWrapping : new boolean[] { false, true }) {
            final FutureTask<String> input = new FutureTask<>(() -> "unused");
            if (beforeWrapping) {
                input.cancel(false);
            }
            final ContinuableFuture<String> delayed = ContinuableFuture.wrap(input).thenDelay(1, TimeUnit.DAYS);
            if (!beforeWrapping) {
                input.cancel(false);
            }
            assertTrue(delayed.isCancelled());
            assertTrue(delayed.isDone());
            assertThrows(CancellationException.class, () -> delayed.get(0, TimeUnit.NANOSECONDS));
            assertThrows(CancellationException.class, () -> delayed.get(-1, TimeUnit.SECONDS));
            assertThrows(CancellationException.class, delayed::get);
            assertThrows(NullPointerException.class, () -> delayed.get(0, null));
        }
    }

    @Test
    void gettersAlreadyWaitingUpstreamObserveCancellationWithoutDelay() throws Exception {
        for (final boolean timed : new boolean[] { false, true }) {
            final CountDownLatch entered = new CountDownLatch(1);
            final FutureTask<String> input = new FutureTask<>(() -> "unused") {
                @Override
                public String get() throws InterruptedException, ExecutionException {
                    entered.countDown();
                    return super.get();
                }

                @Override
                public String get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    entered.countDown();
                    return super.get(timeout, unit);
                }
            };
            final ContinuableFuture<String> delayed = ContinuableFuture.wrap(input).thenDelay(1, TimeUnit.DAYS);
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                final Future<?> getter = executor.submit(() -> {
                    if (timed) {
                        assertThrows(CancellationException.class, () -> delayed.get(1, TimeUnit.DAYS));
                    } else {
                        assertThrows(CancellationException.class, delayed::get);
                    }
                });
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertTrue(delayed.cancel(true));
                getter.get(5, TimeUnit.SECONDS);
                assertTrue(delayed.isDone());
            } finally {
                input.cancel(true);
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void cancellationExceptionAsAnOrdinaryFailureStillObservesTheDelay() throws Exception {
        final CancellationException cause = new CancellationException("task failed");
        final FutureTask<String> input = new FutureTask<>(() -> {
            throw cause;
        });
        input.run();
        final ContinuableFuture<String> delayed = ContinuableFuture.wrap(input).thenDelay(1, TimeUnit.DAYS);
        assertFalse(delayed.isCancelled());
        assertFalse(delayed.isDone());
        assertThrows(TimeoutException.class, () -> delayed.get(0, TimeUnit.NANOSECONDS));
        assertFalse(delayed.isDone());
    }

    @Test
    void unsuccessfulCancellationPreservesSuccessfulCompletionDelay() throws Exception {
        final ContinuableFuture<String> original = ContinuableFuture.completed("\uD83D\uDE00");
        final ContinuableFuture<String> delayed = original.thenDelay(1, TimeUnit.DAYS);
        assertFalse(delayed.cancel(true));
        assertFalse(delayed.isCancelled());
        assertFalse(delayed.isDone());
        assertThrows(TimeoutException.class, () -> delayed.get(0, TimeUnit.NANOSECONDS));
        assertEquals("\uD83D\uDE00", original.get());
        assertFalse(delayed.isDone());
    }
}
