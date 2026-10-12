package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
@Timeout(10)
public class AnyOfInterruptedPollReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(longs = { 0, -1 })
    void interruptedPollCarriesDistinctCheckedErrorAndCancellationFailuresAndCanRetry(final long timeout) throws Exception {
        final Exception first = new Exception("first");
        final AssertionError error = new AssertionError("error");
        final CompletableFuture<String> cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        final InterruptedException interruption = new InterruptedException("poll");
        final AtomicInteger zipCalls = new AtomicInteger();
        final Future<String> interruptOnce = Futures.compose(CompletableFuture.completedFuture("a"), CompletableFuture.completedFuture("b"), (a, b) -> {
            if (zipCalls.incrementAndGet() == 1) {
                throw interruption;
            }
            return "retry";
        });
        final ContinuableFuture<String> any = Futures.anyOf(CompletableFuture.<String> failedFuture(first), CompletableFuture.<String> failedFuture(error),
                cancelled, CompletableFuture.<String> failedFuture(first), interruptOnce);

        try {
            assertSame(interruption, assertThrows(InterruptedException.class, () -> any.get(timeout, TimeUnit.NANOSECONDS)));
            assertTrue(Thread.currentThread().isInterrupted());
            final Throwable[] suppressed = interruption.getSuppressed();
            assertEquals(3, suppressed.length);
            assertSame(first, suppressed[0]);
            assertSame(error, suppressed[1]);
            assertInstanceOf(CancellationException.class, suppressed[2]);
            assertEquals(0, first.getSuppressed().length);
            assertEquals(0, error.getSuppressed().length);
            assertFalse(any.isCancelled());
        } finally {
            Thread.interrupted();
        }
        assertEquals("retry", any.get(0, TimeUnit.NANOSECONDS));
        assertEquals("retry", any.get());
        assertEquals(2, zipCalls.get(), "interruption is nonterminal, while later success is retained");
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1 })
    void reusedInterruptedInstanceIsNeverSuppressedOnItself(final long timeout) {
        final InterruptedException interruption = new InterruptedException("same instance");
        final IllegalArgumentException other = new IllegalArgumentException("other");
        final Future<String> interrupting = Futures.compose(CompletableFuture.completedFuture("a"), CompletableFuture.completedFuture("b"), (a, b) -> {
            throw interruption;
        });
        final ContinuableFuture<String> any = Futures.anyOf(CompletableFuture.<String> failedFuture(interruption),
                CompletableFuture.<String> failedFuture(other), interrupting);
        try {
            assertSame(interruption, assertThrows(InterruptedException.class, () -> any.get(timeout, TimeUnit.SECONDS)));
            assertArrayEquals(new Throwable[] { other }, interruption.getSuppressed());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptedPollIncludesAnEarlierRelayFailureWhoseInputStillReportsPending() throws Exception {
        final IllegalStateException recorded = new IllegalStateException("recorded by relay");
        final CompletableFuture<String> unfinishedSibling = new CompletableFuture<>();
        // combine can fail before all inputs complete, while its isDone() still reports false.
        final Future<String> failedButNotDone = Futures.combine(CompletableFuture.<String> failedFuture(recorded), unfinishedSibling, (a, b) -> a);
        final CompletableFuture<String> gate = new CompletableFuture<>();
        final Thread caller = Thread.currentThread();
        final InterruptedException interruption = new InterruptedException("caller poll");
        final Future<String> interrupting = Futures.compose(gate, CompletableFuture.completedFuture("b"), (a, b) -> {
            a.get();
            if (Thread.currentThread() == caller) {
                throw interruption;
            }
            return "unused relay result";
        });
        final ContinuableFuture<String> any = Futures.anyOf(failedButNotDone, interrupting);
        try {
            boolean observed = false;
            final long start = System.nanoTime();
            while (!observed && System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5)) {
                final TimeoutException expired = assertThrows(TimeoutException.class, () -> any.get(20, TimeUnit.MILLISECONDS));
                observed = Arrays.asList(expired.getSuppressed()).contains(recorded);
            }
            assertTrue(observed, "relay failure recorded before exercising the poll");
            assertFalse(failedButNotDone.isDone(), "the next poll cannot rediscover this failure from its input");
            gate.complete("ready");
            assertSame(interruption, assertThrows(InterruptedException.class, () -> any.get(0, TimeUnit.NANOSECONDS)));
            assertArrayEquals(new Throwable[] { recorded }, interruption.getSuppressed());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0, recorded.getSuppressed().length);
        } finally {
            Thread.interrupted();
            gate.complete("cleanup");
            unfinishedSibling.cancel(false);
        }
    }

    @Test
    void interruptedPollWithoutEarlierFailuresKeepsEmptySuppression() {
        final InterruptedException interruption = new InterruptedException("only failure");
        final Future<String> interrupting = Futures.compose(CompletableFuture.completedFuture("a"), CompletableFuture.completedFuture("b"), (a, b) -> {
            throw interruption;
        });
        try {
            assertSame(interruption, assertThrows(InterruptedException.class, () -> Futures.anyOf(interrupting).get(0, TimeUnit.SECONDS)));
            assertEquals(0, interruption.getSuppressed().length);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void blockingCallerInterruptionStillIncludesObservedFailures() {
        final IllegalStateException first = new IllegalStateException("first");
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(CompletableFuture.<String> failedFuture(first), pending);
        try {
            Thread.currentThread().interrupt();
            final InterruptedException interruption = assertThrows(InterruptedException.class, () -> any.get(1, TimeUnit.SECONDS));
            assertTrue(Thread.currentThread().isInterrupted());
            assertArrayEquals(new Throwable[] { first }, interruption.getSuppressed());
        } finally {
            Thread.interrupted();
            pending.complete("cleanup");
        }
    }

    @Test
    void timeoutPollStillCarriesFailuresWithoutPublishingATerminalOutcome() throws Exception {
        final IllegalStateException first = new IllegalStateException("first");
        final CompletableFuture<String> pending = new CompletableFuture<>();
        final ContinuableFuture<String> any = Futures.anyOf(CompletableFuture.<String> failedFuture(first), pending);
        try {
            final TimeoutException timeout = assertThrows(TimeoutException.class, () -> any.get(0, TimeUnit.NANOSECONDS));
            assertArrayEquals(new Throwable[] { first }, timeout.getSuppressed());
            assertFalse(any.isDone());
            pending.complete("winner");
            assertEquals("winner", any.get(0, TimeUnit.NANOSECONDS));
            assertEquals("winner", any.get());
            assertEquals(0, first.getSuppressed().length);
        } finally {
            pending.complete("cleanup");
        }
    }
}
