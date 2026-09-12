package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class EitherCompletionTest extends TestBase {

    @Test
    void eitherBiConsumerHandlesFailureWhileTheOtherInputRemainsPending() throws Exception {
        for (final boolean reverse : new boolean[] { false, true }) {
            final IOException failure = new IOException("\u65E5\u672C failure");
            final CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(failure);
            final CompletableFuture<String> pending = new CompletableFuture<>();
            final ContinuableFuture<String> first = ContinuableFuture.wrap(reverse ? pending : failed);
            final ContinuableFuture<String> second = ContinuableFuture.wrap(reverse ? failed : pending);
            final AtomicInteger calls = new AtomicInteger();
            try {
                first.runAsyncAfterEither(second, (value, error) -> {
                    calls.incrementAndGet();
                    assertNull(value);
                    assertSame(failure, error);
                }).get(5, TimeUnit.SECONDS);
                assertEquals(1, calls.get());
                assertFalse(pending.isDone());
            } finally {
                pending.complete("cleanup");
            }
        }
    }

    @Test
    void eitherBiConsumerHandlesNullSuccessAndCancellation() throws Exception {
        for (final boolean cancel : new boolean[] { false, true }) {
            final CompletableFuture<String> completed = new CompletableFuture<>();
            if (cancel) {
                completed.cancel(false);
            } else {
                completed.complete(null);
            }
            final CompletableFuture<String> pending = new CompletableFuture<>();
            final AtomicInteger calls = new AtomicInteger();
            try {
                ContinuableFuture.wrap(completed).runAsyncAfterEither(ContinuableFuture.wrap(pending), (value, error) -> {
                    calls.incrementAndGet();
                    assertNull(value);
                    if (cancel) {
                        assertInstanceOf(CancellationException.class, error);
                    } else {
                        assertNull(error);
                    }
                }).get(5, TimeUnit.SECONDS);
                assertEquals(1, calls.get());
                assertFalse(pending.isDone());
            } finally {
                pending.complete("cleanup");
            }
        }
    }

    @Test
    void firstSuccessRemainsTheExplicitSuccessPreferringAlternative() throws Exception {
        final CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IOException("failed"));
        final CompletableFuture<String> successful = new CompletableFuture<>();
        try {
            final ContinuableFuture<Void> result = ContinuableFuture.wrap(failed)
                    .runAsyncAfterFirstSuccess(ContinuableFuture.wrap(successful), (value, error) -> {
                        assertEquals("\uD83D\uDE00", value);
                        assertNull(error);
                    });
            successful.complete("\uD83D\uDE00");
            result.get(5, TimeUnit.SECONDS);
        } finally {
            successful.complete("cleanup");
        }
    }

    @Test
    void eitherBiConsumerPropagatesCallbackFailureAndRejectsNullAction() {
        final IOException failure = new IOException("callback failed");
        final ContinuableFuture<String> ready = ContinuableFuture.completed("");
        final ContinuableFuture<Void> result = ready.runAsyncAfterEither(ready, (value, error) -> {
            throw failure;
        });
        assertSame(failure, assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS)).getCause());
        assertThrows(IllegalArgumentException.class, () -> ready.runAsyncAfterEither(ready, (Throwables.BiConsumer<String, Exception, Exception>) null));
    }

    @Test
    public void testRunAsyncAfterEither_CancelledStageDoesNotRunTheAction() throws Exception {
        final CompletableFuture<String> pendingFirst = new CompletableFuture<>();
        final CompletableFuture<String> pendingSecond = new CompletableFuture<>();
        final ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        final AtomicInteger runs = new AtomicInteger();
        final Throwables.Runnable<Exception> action = runs::incrementAndGet;

        try {
            final ContinuableFuture<Void> stage = ContinuableFuture.wrap(pendingFirst)
                    .thenUse(executor)
                    .runAsyncAfterEither(ContinuableFuture.wrap(pendingSecond), action);

            final Thread worker = executor.awaitSingleWorker();
            awaitParked(worker);

            assertTrue(stage.cancel(true));
            worker.join(TimeUnit.SECONDS.toMillis(WAIT_SECONDS));

            assertFalse(worker.isAlive(), "the combining worker must end after the stage is cancelled");
            assertEquals(0, runs.get());
            assertFalse(pendingFirst.isDone());
            assertFalse(pendingSecond.isDone());
            assertThrows(CancellationException.class, () -> stage.get(WAIT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pendingFirst.complete("cleanup");
            pendingSecond.complete("cleanup");
        }
    }

    @Test
    public void testRunAsyncAfterEither_InterruptedWorkerDoesNotFabricateAnOutcome() throws Exception {
        final CompletableFuture<String> pendingFirst = new CompletableFuture<>();
        final CompletableFuture<String> pendingSecond = new CompletableFuture<>();
        final ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        final AtomicInteger calls = new AtomicInteger();

        try {
            final ContinuableFuture<Void> stage = ContinuableFuture.wrap(pendingFirst)
                    .thenUse(executor)
                    .runAsyncAfterEither(ContinuableFuture.wrap(pendingSecond), (value, error) -> calls.incrementAndGet());

            final Thread worker = executor.awaitSingleWorker();
            awaitParked(worker);
            worker.interrupt();

            final ExecutionException failure = assertThrows(ExecutionException.class, () -> stage.get(WAIT_SECONDS, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertEquals(0, calls.get());
            assertFalse(pendingFirst.isDone());
            assertFalse(pendingSecond.isDone());
        } finally {
            pendingFirst.complete("cleanup");
            pendingSecond.complete("cleanup");
        }
    }

    @Test
    public void testCallAsyncAfterFirstSuccess_InterruptedWorkerReportsTheInterruption() throws Exception {
        final CompletableFuture<String> pendingFirst = new CompletableFuture<>();
        final CompletableFuture<String> pendingSecond = new CompletableFuture<>();
        final ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        final AtomicInteger calls = new AtomicInteger();

        try {
            final ContinuableFuture<String> stage = ContinuableFuture.wrap(pendingFirst)
                    .thenUse(executor)
                    .callAsyncAfterFirstSuccess(ContinuableFuture.wrap(pendingSecond), () -> {
                        calls.incrementAndGet();
                        return "done";
                    });

            final Thread worker = executor.awaitSingleWorker();
            awaitParked(worker);
            worker.interrupt();

            final ExecutionException failure = assertThrows(ExecutionException.class, () -> stage.get(WAIT_SECONDS, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertEquals(0, calls.get());
            assertFalse(pendingFirst.isDone());
            assertFalse(pendingSecond.isDone());
        } finally {
            pendingFirst.complete("cleanup");
            pendingSecond.complete("cleanup");
        }
    }

    @Test
    public void testRunAsyncAfterFirstSuccess_InterruptedWorkerDoesNotCallTheBiConsumer() throws Exception {
        final CompletableFuture<String> pendingFirst = new CompletableFuture<>();
        final CompletableFuture<String> pendingSecond = new CompletableFuture<>();
        final ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        final AtomicInteger calls = new AtomicInteger();

        try {
            final ContinuableFuture<Void> stage = ContinuableFuture.wrap(pendingFirst)
                    .thenUse(executor)
                    .runAsyncAfterFirstSuccess(ContinuableFuture.wrap(pendingSecond), (value, error) -> calls.incrementAndGet());

            final Thread worker = executor.awaitSingleWorker();
            awaitParked(worker);
            worker.interrupt();

            final ExecutionException failure = assertThrows(ExecutionException.class, () -> stage.get(WAIT_SECONDS, TimeUnit.SECONDS));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertEquals(0, calls.get());
            assertFalse(pendingFirst.isDone());
            assertFalse(pendingSecond.isDone());
        } finally {
            pendingFirst.complete("cleanup");
            pendingSecond.complete("cleanup");
        }
    }

    private static final long WAIT_SECONDS = 5;

    /**
     * Runs each submitted task on its own daemon thread and records it, so a test can get hold of the thread the
     * combining stage runs on and interrupt exactly that one.
     */
    private static final class ThreadPerTaskExecutor implements Executor {
        private final List<Thread> threads = new CopyOnWriteArrayList<>();

        @Override
        public void execute(final Runnable command) {
            final Thread thread = new Thread(command, "either-completion-worker");
            thread.setDaemon(true);
            thread.start();
            threads.add(thread);
        }

        Thread awaitSingleWorker() throws InterruptedException {
            final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);

            while (threads.isEmpty()) {
                assertTrue(System.nanoTime() - deadlineNanos < 0, "no task was submitted to the executor");
                Thread.sleep(1);
            }

            assertEquals(1, threads.size());

            return threads.get(0);
        }
    }

    /** Waits, bounded, until the combining worker is blocked - it parks in the outcome queue's take(). */
    private static void awaitParked(final Thread thread) throws InterruptedException {
        final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        Thread.State state = thread.getState();

        while (state != Thread.State.WAITING && state != Thread.State.TIMED_WAITING) {
            assertTrue(System.nanoTime() - deadlineNanos < 0, "the combining worker never parked; state=" + state);
            Thread.sleep(1);
            state = thread.getState();
        }
    }
}
