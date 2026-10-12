package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;

@Tag("unit")
class StreamParallelConcatCleanupReview20261009Test extends TestBase {

    @Test
    void closesTemporaryExecutorWhenPipelineClosesDuringStartup() throws Exception {
        final AsyncExecutor temporary = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);
        final ExecutorService pool = (ExecutorService) temporary.getExecutor();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final Stream<Integer> combined = Stream.parallelConcat(List.of(Stream.of(1).onClose(sourceCloses::incrementAndGet)), 1, 1);
        final CountDownLatch startup = new CountDownLatch(1);
        final CompletableFuture<Void> closer = CompletableFuture.runAsync(() -> {
            await(startup);
            combined.close();
        });

        try (MockedStatic<StreamBase> methods = mockStatic(StreamBase.class, CALLS_REAL_METHODS)) {
            // Force the consumer to close while the producer is acquiring its executor, before that owner is published.
            methods.when(() -> StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 1)).thenAnswer(invocation -> {
                startup.countDown();
                closer.get(5, TimeUnit.SECONDS);
                return temporary;
            });

            assertEquals(List.of(), combined.toList());
            assertTrue(pool.isShutdown(), "the executor returned after pipeline closure must still be shut down");
            assertEquals(1, sourceCloses.get());
        } finally {
            startup.countDown();
            closer.get(5, TimeUnit.SECONDS);
            combined.close();
            temporary.shutdown();
        }
    }

    @Test
    void closesTemporaryExecutorWhenTheReaderStopsRightAfterALateChild() throws Exception {
        final AsyncExecutor temporary = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);
        final ExecutorService pool = (ExecutorService) temporary.getExecutor();
        final CountDownLatch firstQueued = new CountDownLatch(1);
        final Iterator<Integer> source = new Iterator<>() {
            private int next;

            @Override
            public boolean hasNext() {
                if (next == 1) {
                    firstQueued.countDown(); // element 1 has been offered to the (size 1) buffer
                }

                return next < 2;
            }

            @Override
            public Integer next() {
                return ++next;
            }
        };
        final Stream<Integer> combined = Stream.parallelConcat(List.of(Stream.of(source)), 1, 1);

        try (MockedStatic<StreamBase> methods = mockStatic(StreamBase.class, CALLS_REAL_METHODS)) {
            methods.when(() -> StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 1)).thenReturn(temporary);
            // Close the pipeline after the worker has queued an element but before the reader publishes the child that
            // owns the executor - the moment a buffered() consumer on another thread can close it.
            methods.when(() -> StreamBase.execute(any(AsyncExecutor.class), anyInt(), anyInt(), anyList(), any(Runnable.class))).thenAnswer(invocation -> {
                final Object result = invocation.callRealMethod();
                await(firstQueued);
                combined.close();
                return result;
            });

            final Iterator<Integer> iterator = combined.iterator();

            // The reader is not traversed any further, as a stopped buffered() reader would not be; the queued element
            // used to keep the late child open, so nothing ever shut the temporary executor down.
            assertFalse(iterator.hasNext(), "cancellation must reclaim the late child without traversing its queued element");
            assertTrue(pool.isShutdown(), "a child created after pipeline closure must release its temporary executor");
        } finally {
            combined.close();
            temporary.shutdown();
        }
    }

    @Test
    void closesTemporaryExecutorWhenWorkerSubmissionFails() {
        final RejectedExecutionException failure = new RejectedExecutionException("worker rejected");
        final AsyncExecutor temporary = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS) {
            @Override
            public <R> ContinuableFuture<R> execute(final Callable<? extends R> command) {
                throw failure;
            }
        };
        final ExecutorService pool = (ExecutorService) temporary.getExecutor();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final Stream<Integer> combined = Stream.parallelConcat(List.of(Stream.of(1).onClose(sourceCloses::incrementAndGet)), 1, 1);

        try (MockedStatic<StreamBase> methods = mockStatic(StreamBase.class, CALLS_REAL_METHODS)) {
            methods.when(() -> StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 1)).thenReturn(temporary);

            assertSame(failure, assertThrows(RejectedExecutionException.class, combined::toList));
            assertTrue(pool.isShutdown(), "startup failure must release an executor before any child iterator is returned");
            assertEquals(1, sourceCloses.get());
        } finally {
            combined.close();
            temporary.shutdown();
        }
    }

    @Test
    void ordinaryTraversalClosesTemporaryExecutorAndSources() {
        final AsyncExecutor temporary = new AsyncExecutor(1, 1, 1, TimeUnit.SECONDS);
        final ExecutorService pool = (ExecutorService) temporary.getExecutor();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final Stream<Integer> combined = Stream
                .parallelConcat(List.of(Stream.of(1, 2).onClose(sourceCloses::incrementAndGet), Stream.of(3).onClose(sourceCloses::incrementAndGet)), 1, 1);

        try (MockedStatic<StreamBase> methods = mockStatic(StreamBase.class, CALLS_REAL_METHODS)) {
            methods.when(() -> StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 1)).thenReturn(temporary);

            assertEquals(List.of(1, 2, 3), combined.toList());
            assertTrue(pool.isShutdown());
            assertEquals(2, sourceCloses.get());
        } finally {
            combined.close();
            temporary.shutdown();
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "parallel concat startup did not reach its checkpoint");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
