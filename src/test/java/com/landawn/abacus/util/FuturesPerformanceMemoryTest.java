package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Futures. */
@Tag("slow-test")
public class FuturesPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G030 end ----
    // ---- perf review 2026-10-08 G031 begin ----
    @Nested
    class G031 {

        /**
         * An input whose {@code isDone()} answers {@code false} on its first call only (or throws {@code isDoneFailure}
         * from the second call on); its timed {@code get} yields {@code value} or fails with {@code failure}.
         */
        private final class FlipFuture implements java.util.concurrent.Future<String> {
            private final AtomicInteger isDoneCalls = new AtomicInteger();
            private final AtomicInteger getCalls = new AtomicInteger();
            private final String value;
            private final Exception failure;
            private final RuntimeException isDoneFailure;

            FlipFuture(final String value, final Exception failure, final RuntimeException isDoneFailure) {
                this.value = value;
                this.failure = failure;
                this.isDoneFailure = isDoneFailure;
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                if (isDoneCalls.incrementAndGet() == 1) {
                    return false;
                }
                if (isDoneFailure != null) {
                    throw isDoneFailure;
                }
                return true;
            }

            @Override
            public String get() throws java.util.concurrent.ExecutionException {
                return get(0, java.util.concurrent.TimeUnit.NANOSECONDS);
            }

            @Override
            public String get(final long timeout, final java.util.concurrent.TimeUnit unit) throws java.util.concurrent.ExecutionException {
                getCalls.incrementAndGet();
                if (failure != null) {
                    throw new java.util.concurrent.ExecutionException(failure);
                }
                return value;
            }
        }

        // G031-01: a pending anyOf aggregate answers isDone() == false on every poll without registering an observer,
        // and an input's success is published by the next poll.
        @Test
        void testAnyOfIsDone_pendingThenSuccess() throws Exception {
            final java.util.concurrent.CompletableFuture<String> first = new java.util.concurrent.CompletableFuture<>();
            final java.util.concurrent.CompletableFuture<String> second = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(first, second);

            for (int i = 0; i < 100; i++) {
                assertFalse(any.isDone());
            }

            assertFalse(any.isCancelled());
            assertFalse(Thread.currentThread().isInterrupted());
            assertEquals(0, first.getNumberOfDependents(), "isDone() polls; it never registers a completion listener");
            assertEquals(0, second.getNumberOfDependents());
            assertThrows(java.util.concurrent.TimeoutException.class, () -> any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));

            second.complete("second");
            assertTrue(any.isDone());
            first.complete("first");
            assertTrue(any.isDone());
            assertEquals("second", any.get());
            assertEquals("second", any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
        }

        // G031-01: isDone() polls of a failed + pending aggregate attach nothing to anything; the non-positive-timeout
        // get still throws a TimeoutException carrying the failure exactly once, before and after a blocking getter
        // recorded it.
        @Test
        void testAnyOfIsDone_failedAndPendingKeepsTimeoutContract() throws Exception {
            final IllegalStateException failure = new IllegalStateException("first");
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(java.util.concurrent.CompletableFuture.<String> failedFuture(failure), pending);

            for (int i = 0; i < 50; i++) {
                assertFalse(any.isDone());
            }

            assertEquals(0, failure.getSuppressed().length);

            for (final long timeout : new long[] { 0, -1 }) {
                final java.util.concurrent.TimeoutException expired = assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> any.get(timeout, java.util.concurrent.TimeUnit.NANOSECONDS));
                assertArrayEquals(new Throwable[] { failure }, expired.getSuppressed());
            }

            final java.util.concurrent.TimeoutException blockingExpired = assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> any.get(20, java.util.concurrent.TimeUnit.MILLISECONDS));
            assertArrayEquals(new Throwable[] { failure }, blockingExpired.getSuppressed());

            for (int i = 0; i < 10; i++) {
                assertFalse(any.isDone());
            }

            final java.util.concurrent.TimeoutException polled = assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
            assertArrayEquals(new Throwable[] { failure }, polled.getSuppressed());
            assertEquals(0, failure.getSuppressed().length);
            assertFalse(any.isCancelled());

            pending.complete("late");
            assertTrue(any.isDone());
            assertEquals("late", any.get());
        }

        // G031-01: when every input turns out failed during the isDone() poll, the aggregate is done and get() reports
        // the first failure (input order) with the others suppressed; later polls answer without reading the inputs.
        @Test
        void testAnyOfIsDone_allFailedDuringPollPublishesFailure() {
            final IllegalArgumentException first = new IllegalArgumentException("first");
            final IllegalStateException second = new IllegalStateException("second");
            final FlipFuture flipping = new FlipFuture(null, first, null);
            final ContinuableFuture<String> any = Futures.anyOf(flipping, java.util.concurrent.CompletableFuture.<String> failedFuture(second));

            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());

            final java.util.concurrent.ExecutionException e = assertThrows(java.util.concurrent.ExecutionException.class, any::get);
            assertSame(first, e.getCause());
            assertArrayEquals(new Throwable[] { second }, first.getSuppressed());
            assertFalse(any.isCancelled());
            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get(), "a published outcome answers without polling again");
            assertEquals(1, flipping.getCalls.get());
        }

        // G031-01: a success read by the isDone() poll is published once and retained; nothing is registered on the
        // still-pending sibling.
        @Test
        void testAnyOfIsDone_successReadByPollIsPublished() throws Exception {
            final FlipFuture flipping = new FlipFuture("polled", null, null);
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(flipping, pending);

            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());
            assertEquals("polled", any.get());
            assertEquals("polled", any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());
            assertEquals(0, pending.getNumberOfDependents());
        }

        // G031-01: an InterruptedException met by the isDone() poll answers false, restores the interrupt flag, carries
        // the failures observed by that poll, and leaves the aggregate retryable.
        @Test
        void testAnyOfIsDone_interruptedPoll() throws Exception {
            final IllegalStateException failure = new IllegalStateException("first");
            final InterruptedException interruption = new InterruptedException("poll");
            final AtomicInteger zipCalls = new AtomicInteger();
            final java.util.concurrent.Future<String> interruptOnce = Futures.compose(java.util.concurrent.CompletableFuture.completedFuture("a"),
                    java.util.concurrent.CompletableFuture.completedFuture("b"), (a, b) -> {
                        if (zipCalls.incrementAndGet() == 1) {
                            throw interruption;
                        }
                        return "retry";
                    });
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(java.util.concurrent.CompletableFuture.<String> failedFuture(failure), interruptOnce, pending);

            try {
                assertFalse(any.isDone());
                assertTrue(Thread.currentThread().isInterrupted());
                assertArrayEquals(new Throwable[] { failure }, interruption.getSuppressed());
                assertEquals(0, failure.getSuppressed().length);
                assertEquals(1, zipCalls.get());
            } finally {
                Thread.interrupted();
            }

            assertTrue(any.isDone());
            assertEquals(2, zipCalls.get());
            assertEquals("retry", any.get());
            assertEquals(0, pending.getNumberOfDependents());
        }

        // G031-01: an unchecked exception thrown by an input's isDone() during the poll keeps its old outcome:
        // a CancellationException reads as done, anything else propagates unchanged.
        @Test
        void testAnyOfIsDone_inputIsDoneThrowsDuringPoll() {
            final FlipFuture cancelling = new FlipFuture(null, null, new java.util.concurrent.CancellationException("from isDone"));
            assertTrue(Futures.anyOf(cancelling, new java.util.concurrent.CompletableFuture<String>()).isDone());
            assertEquals(2, cancelling.isDoneCalls.get());

            final IllegalStateException boom = new IllegalStateException("boom");
            final FlipFuture failing = new FlipFuture(null, null, boom);
            final ContinuableFuture<String> any = Futures.anyOf(failing, new java.util.concurrent.CompletableFuture<String>());
            assertSame(boom, assertThrows(IllegalStateException.class, any::isDone));
            assertEquals(0, failing.getCalls.get());
        }
    }
}
