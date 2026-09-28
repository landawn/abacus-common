package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class NAsyncTest extends NTestSupport {

    @Test
    public void testCustomExecutorMayCompleteBeforeReturn() throws Exception {
        java.util.concurrent.Executor direct = Runnable::run;
        Thread caller = Thread.currentThread();
        List<Thread> executions = new ArrayList<>();
        Throwables.Runnable<Exception> runnable = () -> {
            executions.add(Thread.currentThread());
        };
        Callable<Thread> callable = () -> {
            executions.add(Thread.currentThread());
            return Thread.currentThread();
        };

        ContinuableFuture<Void> run = N.asyncExecute(runnable, direct);
        assertTrue(run.isDone());
        assertEquals(List.of(caller), executions);
        ContinuableFuture<Thread> call = N.asyncExecute(callable, direct);
        assertTrue(call.isDone());
        assertEquals(caller, call.get());
        assertTrue(N.asyncExecute(List.of(runnable), direct).get(0).isDone());
        assertTrue(N.asyncExecuteAll(List.of(runnable), direct).get(0).isDone());
        assertTrue(N.asyncExecute(List.of(callable), direct).get(0).isDone());
        ObjIterator<Void> runs = N.runAsync(List.of(runnable), direct);
        ObjIterator<Thread> calls = N.callAsync(List.of(callable), direct);

        assertEquals(Collections.nCopies(7, caller), executions);
        assertNull(runs.next());
        assertFalse(runs.hasNext());
        assertEquals(caller, calls.next());
        assertFalse(calls.hasNext());
    }

    @Test
    public void testAsyncExecute_runnableAndCallable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.asyncExecute(() -> executed.set(true)).get();
        assertTrue(executed.get());

        assertEquals("result", N.asyncExecute(() -> "result").get());
        assertEquals("done", N.asyncExecute(() -> "done", executorService).get());

        List<Throwables.Runnable<Exception>> runnables = Arrays.asList(() -> executed.set(true), () -> executed.set(true));
        List<ContinuableFuture<Void>> futures = N.asyncExecute(runnables);
        assertEquals(2, futures.size());
        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }

        List<Throwables.Runnable<RuntimeException>> one = CommonUtil.asList((Throwables.Runnable<RuntimeException>) () -> {
        });
        N.asyncExecute(one).get(0).get();

        List<Callable<Void>> callables = CommonUtil.toList((Callable<Void>) () -> null);
        assertNull(N.asyncExecute(callables).get(0).get());
    }

    @Test
    public void testAsyncExecute_delayAndCollection() throws Exception {
        long start = System.currentTimeMillis();
        N.asyncExecute(() -> {
        }, 50).get();
        assertTrue(System.currentTimeMillis() - start >= 50);

        AtomicInteger counter = new AtomicInteger();
        List<ContinuableFuture<Void>> futures = N.asyncExecute(
                Arrays.asList((Throwables.Runnable<Exception>) counter::incrementAndGet, (Throwables.Runnable<Exception>) counter::incrementAndGet),
                executorService);
        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }
        assertEquals(2, counter.get());

        Collection<Throwables.Runnable<Exception>> commands = new LinkedHashSet<>();
        commands.add(() -> counter.addAndGet(1));
        commands.add(() -> counter.addAndGet(10));
        int before = counter.get();
        for (ContinuableFuture<Void> future : N.asyncExecuteAll(commands)) {
            future.get();
        }
        assertEquals(before + 11, counter.get());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Integer> executed = Collections.synchronizedList(new ArrayList<>());
            Collection<Throwables.Runnable<Exception>> ordered = new LinkedHashSet<>();
            ordered.add(() -> executed.add(1));
            ordered.add(() -> executed.add(2));
            ordered.add(() -> executed.add(3));
            for (ContinuableFuture<Void> future : N.asyncExecuteAll(ordered, executor)) {
                future.get();
            }
            assertEquals(Arrays.asList(1, 2, 3), executed);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testAsyncExecute_nullEmptyRetryCancel() throws Exception {
        assertTrue(N.asyncExecuteAll((Collection<Throwables.Runnable<Exception>>) null).isEmpty());
        assertTrue(N.asyncExecuteAll(Collections.emptySet(), executorService).isEmpty());
        assertTrue(N.asyncExecute((List<Throwables.Runnable<Exception>>) null).isEmpty());
        assertTrue(N.asyncExecute((Collection<Callable<String>>) null).isEmpty());
        assertTrue(N.asyncExecute(Collections.<Throwables.Runnable<Exception>> emptyList()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(() -> {
        }, null));

        AtomicInteger attempts = new AtomicInteger();
        assertEquals("success", N.asyncExecute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Retry");
            }
            return "success";
        }, 3, 10, (result, exception) -> exception != null).get());
        assertEquals(3, attempts.get());

        AtomicInteger failures = new AtomicInteger();
        assertThrows(ExecutionException.class, () -> N.asyncExecute(() -> {
            failures.incrementAndGet();
            throw new RuntimeException("Always fails");
        }, 3, 10, e -> true).get());
        assertEquals(4, failures.get());

        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
        latch.countDown();

        assertEquals("Hello World!", N.asyncExecute(() -> "Hello").getThenApply(s -> s + " World!"));
        assertThrows(ExecutionException.class, () -> N.asyncExecute(() -> {
            throw new RuntimeException("Test exception");
        }).get());

        List<Object> mixed = new ArrayList<>();
        List<Callable<Object>> callables = Arrays.asList(() -> "string", () -> 42, () -> true, () -> null);
        for (ContinuableFuture<Object> item : N.asyncExecute(callables)) {
            mixed.add(item.get());
        }
        assertEquals(Arrays.asList("string", 42, true, null), mixed);
    }

    /** A {@code Collection} whose {@code size()} deliberately disagrees with what its iterator yields. */
    private static final class InconsistentSizeCollection<T> extends java.util.AbstractCollection<T> {
        private final List<T> elements;

        private final int reportedSize;

        InconsistentSizeCollection(final List<T> elements, final int reportedSize) {
            this.elements = elements;
            this.reportedSize = reportedSize;
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return elements.iterator();
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }

    @Test
    public void testRunAsync_collectionIteratorDecidesTheCompletionCount() {
        final java.util.concurrent.Executor direct = Runnable::run;
        final AtomicInteger ran = new AtomicInteger();
        final List<Throwables.Runnable<? extends Exception>> twoCommands = Arrays.<Throwables.Runnable<? extends Exception>> asList(ran::incrementAndGet,
                ran::incrementAndGet);

        // size() smaller than the traversal: every command that ran must still publish its completion.
        final ObjIterator<Void> iter = N.runAsync(new InconsistentSizeCollection<>(twoCommands, 1), direct);
        int observed = 0;

        while (iter.hasNext()) {
            iter.next();
            observed++;
        }

        assertEquals(2, ran.get());
        assertEquals(2, observed);

        // size() larger than the traversal: the iterator must end instead of waiting forever for a completion
        // that is never published.
        final AtomicInteger ranOnce = new AtomicInteger();
        final List<Throwables.Runnable<? extends Exception>> oneCommand = Arrays.<Throwables.Runnable<? extends Exception>> asList(ranOnce::incrementAndGet);

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            final ObjIterator<Void> it = N.runAsync(new InconsistentSizeCollection<>(oneCommand, 2), direct);
            int seen = 0;

            while (it.hasNext()) {
                it.next();
                seen++;
            }

            assertEquals(1, seen);
            assertEquals(1, ranOnce.get());
        });
    }

    @Test
    public void testCallAsync_collectionIteratorDecidesTheCompletionCount() {
        final java.util.concurrent.Executor direct = Runnable::run;
        final List<Callable<String>> twoCommands = Arrays.<Callable<String>> asList(() -> "a", () -> "b");

        // size() smaller than the traversal: no command's result may be discarded.
        final ObjIterator<String> iter = N.callAsync(new InconsistentSizeCollection<>(twoCommands, 1), direct);
        final List<String> results = new ArrayList<>();

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(Arrays.asList("a", "b"), results);

        // size() larger than the traversal: the iterator must end rather than block on a missing completion.
        final List<Callable<String>> oneCommand = Arrays.<Callable<String>> asList(() -> "only");

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            final ObjIterator<String> it = N.callAsync(new InconsistentSizeCollection<>(oneCommand, 2), direct);
            final List<String> seen = new ArrayList<>();

            while (it.hasNext()) {
                seen.add(it.next());
            }

            assertEquals(Arrays.asList("only"), seen);
        });
    }


    @Test
    public void testAsyncExecuteWithRetry_invalidRetryArgumentsRejectedOnCallingThread() {
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(() -> {
        }, -1, 0, e -> false));
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(() -> {
        }, 1, -1, e -> false));
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(() -> "x", -1, 0, (r, e) -> false));
        assertThrows(IllegalArgumentException.class, () -> N.asyncExecute(() -> "x", 1, -1, (r, e) -> false));
    }

    @Test
    public void testRunAsyncCallAsync_workerInterruptedExceptionDoesNotInterruptConsumer() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            final List<Throwables.Runnable<Exception>> commands = new ArrayList<>();
            commands.add(() -> {
                throw new InterruptedException("worker");
            });

            Thread.interrupted();
            final ObjIterator<Void> runIter = N.runAsync(commands, executor);
            assertThrows(RuntimeException.class, () -> {
                while (runIter.hasNext()) {
                    runIter.next();
                }
            });
            assertFalse(Thread.currentThread().isInterrupted());

            final List<Callable<String>> calls = new ArrayList<>();
            calls.add(() -> {
                throw new InterruptedException("worker");
            });

            final ObjIterator<String> callIter = N.callAsync(calls, executor);
            assertThrows(RuntimeException.class, () -> {
                while (callIter.hasNext()) {
                    callIter.next();
                }
            });
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testRunAsyncCallAsync_directExecutorInterruptedExceptionRestoresInterrupt() {
        final Executor direct = Runnable::run;

        try {
            final List<Throwables.Runnable<Exception>> commands = new ArrayList<>();
            commands.add(() -> {
                throw new InterruptedException("caller");
            });

            final ObjIterator<Void> runIter = N.runAsync(commands, direct);
            assertThrows(RuntimeException.class, () -> {
                while (runIter.hasNext()) {
                    runIter.next();
                }
            });
            assertTrue(Thread.interrupted());

            final List<Callable<String>> calls = new ArrayList<>();
            calls.add(() -> {
                throw new InterruptedException("caller");
            });

            final ObjIterator<String> callIter = N.callAsync(calls, direct);
            assertThrows(RuntimeException.class, () -> {
                while (callIter.hasNext()) {
                    callIter.next();
                }
            });
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testForEachInParallel_workerInterruptedExceptionDoesNotInterruptCaller() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Thread.interrupted();
            assertThrows(RuntimeException.class, () -> N.forEachInParallel(Arrays.asList(1, 2, 3), v -> {
                throw new InterruptedException("worker");
            }, 2, executor));
            assertFalse(Thread.currentThread().isInterrupted());

            assertThrows(RuntimeException.class, () -> N.forEachIndexedInParallel(Arrays.asList(1, 2, 3), (idx, v) -> {
                throw new InterruptedException("worker");
            }, 2, executor));
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testForEachInParallel_directExecutorInterruptedExceptionRestoresInterrupt() {
        final Executor direct = Runnable::run;

        try {
            final RuntimeException e1 = assertThrows(RuntimeException.class, () -> N.forEachInParallel(Arrays.asList(1, 2, 3), v -> {
                throw new InterruptedException("caller");
            }, 2, direct));
            assertEquals("caller", e1.getCause().getMessage());
            assertTrue(Thread.interrupted());

            assertThrows(RuntimeException.class, () -> N.forEachIndexedInParallel(Arrays.asList(1, 2, 3), (idx, v) -> {
                throw new InterruptedException("caller");
            }, 2, direct));
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
