package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class FutureTerminalOutcomeTest {
    @Test
    void mixedGetFormsAndIsDoneRetainTheFirstObservedSuccessIncludingNull() throws Exception {
        for (boolean nullValue : new boolean[] { false, true }) {
            CompletableFuture<Integer> first = new CompletableFuture<>();
            CompletableFuture<Integer> second = new CompletableFuture<>();
            ContinuableFuture<Integer> any = Futures.anyOf(first, second);
            assertFalse(any.isDone());
            second.complete(nullValue ? null : 2);
            assertTrue(any.isDone());
            first.complete(1);
            Integer expected = nullValue ? null : 2;
            assertEquals(expected, any.get());
            assertEquals(expected, any.get(0, TimeUnit.NANOSECONDS));
            assertEquals(expected, any.get(-1, TimeUnit.SECONDS));
            assertEquals(expected, any.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void failuresAndCancellationAreStableButCallerTimeoutAndInterruptionAreNotCached() throws Exception {
        IllegalArgumentException first = new IllegalArgumentException("first");
        IllegalStateException second = new IllegalStateException("second");
        ContinuableFuture<Integer> failed = Futures.anyOf(CompletableFuture.<Integer> failedFuture(first), CompletableFuture.<Integer> failedFuture(second));
        ExecutionException published = assertThrows(ExecutionException.class, () -> failed.get(0, TimeUnit.NANOSECONDS));
        int suppression = first.getSuppressed().length + second.getSuppressed().length;
        assertEquals(1, suppression);
        assertSame(published, assertThrows(ExecutionException.class, failed::get));
        assertSame(published, assertThrows(ExecutionException.class, () -> failed.get(1, TimeUnit.SECONDS)));
        assertEquals(suppression, first.getSuppressed().length + second.getSuppressed().length);
        CompletableFuture<Integer> cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        ContinuableFuture<Integer> allCancelled = Futures.anyOf(cancelled, cancelled);
        CancellationException cancellation = assertThrows(CancellationException.class, allCancelled::get);
        assertSame(cancellation, assertThrows(CancellationException.class, () -> allCancelled.get(0, TimeUnit.NANOSECONDS)));
        assertTrue(allCancelled.isCancelled());
        CompletableFuture<Integer> pending = new CompletableFuture<>();
        ContinuableFuture<Integer> eventual = Futures.anyOf(cancelled, pending);
        assertThrows(TimeoutException.class, () -> eventual.get(1, TimeUnit.MILLISECONDS));
        try {
            Thread.currentThread().interrupt();
            assertThrows(InterruptedException.class, eventual::get);
        } finally {
            Thread.interrupted();
        }
        pending.complete(7);
        assertEquals(7, eventual.get(1, TimeUnit.SECONDS));
    }

    @Test
    void competingSuccessPublicationsConverge() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<Integer> slowRead = new Future<>() {
            @Override
            public boolean cancel(boolean interrupt) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public Integer get() throws InterruptedException {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return 2;
            }

            @Override
            public Integer get(long timeout, TimeUnit unit) throws InterruptedException {
                return get();
            }
        };
        CompletableFuture<Integer> first = new CompletableFuture<>();
        ContinuableFuture<Integer> any = Futures.anyOf(first, slowRead);
        AtomicReference<Object> observed = new AtomicReference<>();
        Thread slower = new Thread(() -> {
            try {
                observed.set(any.get(0, TimeUnit.NANOSECONDS));
            } catch (Throwable error) {
                observed.set(error);
            }
        });
        try {
            slower.start();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            // The first caller has selected the second input but has not published it yet.
            first.complete(1);
            assertEquals(1, any.get(0, TimeUnit.NANOSECONDS));
            release.countDown();
            slower.join(6000);
            assertFalse(slower.isAlive());
            assertEquals(1, observed.get());
        } finally {
            release.countDown();
        }
    }
}
