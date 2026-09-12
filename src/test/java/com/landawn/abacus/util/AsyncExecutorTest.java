package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AsyncExecutorTest extends TestBase {

    private static void awaitTerminated(final AsyncExecutor executor) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!executor.isTerminated() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testConstructors() {
        AsyncExecutor def = new AsyncExecutor();
        assertNotNull(def.getExecutor());
        def.shutdown();

        AsyncExecutor parameterized = new AsyncExecutor(4, 8, 120L, TimeUnit.SECONDS);
        assertTrue(parameterized.toString().contains("coreThreadPoolSize: 4"));
        assertTrue(parameterized.toString().contains("maxThreadPoolSize: 8"));
        parameterized.shutdown();

        AsyncExecutor maxLessThanCore = new AsyncExecutor(8, 4, 60L, TimeUnit.SECONDS);
        assertTrue(maxLessThanCore.toString().contains("maxThreadPoolSize: 8"));
        maxLessThanCore.shutdown();

        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        AsyncExecutor wrapped = new AsyncExecutor(tpe);
        assertSame(tpe, wrapped.getExecutor());
        wrapped.shutdown();

        Executor plain = Runnable::run;
        AsyncExecutor fromPlain = new AsyncExecutor(plain);
        assertNotNull(fromPlain);
        fromPlain.shutdown();

        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor((Executor) null));
        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor(-1, 4, 60L, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor(2, -1, 60L, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor(0, 0, 60L, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor(2, 4, -1L, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> new AsyncExecutor(2, 4, 60L, null));
    }

    @Test
    public void testExecuteRunnable() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        try {
            AtomicInteger counter = new AtomicInteger();
            executor.execute((Throwables.Runnable<Exception>) () -> counter.incrementAndGet()).get();
            assertEquals(1, counter.get());

            ContinuableFuture<Void> failing = executor.execute((Throwables.Runnable<Exception>) () -> {
                throw new RuntimeException("Test exception");
            });
            assertThrows(Exception.class, failing::get);

            AtomicInteger finalCounter = new AtomicInteger();
            executor.execute((Throwables.Runnable<Exception>) () -> counter.incrementAndGet(), finalCounter::incrementAndGet).get();
            assertEquals(2, counter.get());
            assertEquals(1, finalCounter.get());

            AtomicBoolean finalExecuted = new AtomicBoolean();
            ContinuableFuture<Void> failWithFinal = executor.execute((Throwables.Runnable<Exception>) () -> {
                throw new RuntimeException("Test exception");
            }, () -> finalExecuted.set(true));
            assertThrows(Exception.class, failWithFinal::get);
            assertTrue(finalExecuted.get());

            executor.execute(Fnn.r(() -> counter.incrementAndGet()), () -> finalCounter.incrementAndGet()).get();
            assertEquals(3, counter.get());
            assertEquals(2, finalCounter.get());

            assertTrue(executor.execute((List<Throwables.Runnable<? extends Exception>>) null).isEmpty());
            assertTrue(executor.execute(new ArrayList<Throwables.Runnable<? extends Exception>>()).isEmpty());

            List<Throwables.Runnable<? extends Exception>> tasks = Arrays.asList(() -> counter.incrementAndGet(), () -> counter.incrementAndGet(),
                    () -> counter.incrementAndGet());
            List<ContinuableFuture<Void>> futures = executor.execute(tasks);
            assertEquals(3, futures.size());
            for (ContinuableFuture<Void> future : futures) {
                future.get();
            }
            assertEquals(6, counter.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testExecuteCallable() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        try {
            assertEquals("Hello World", executor.execute(() -> "Hello World").get());
            assertNull(executor.execute(() -> (String) null).get());
            assertEquals(5050, executor.execute(() -> {
                int sum = 0;
                for (int i = 1; i <= 100; i++) {
                    sum += i;
                }
                return sum;
            }).get());
            assertEquals(25, executor.execute(() -> 10).thenCallAsync(x -> x * 2).thenCallAsync(x -> x + 5).get());

            ContinuableFuture<String> failing = executor.execute(() -> {
                throw new RuntimeException("Test exception");
            });
            assertThrows(Exception.class, failing::get);

            AtomicInteger finalCounter = new AtomicInteger();
            assertEquals(42, executor.execute(() -> 42, finalCounter::incrementAndGet).get());
            assertEquals(1, finalCounter.get());

            AtomicBoolean finalExecuted = new AtomicBoolean();
            ContinuableFuture<String> failWithFinal = executor.execute(() -> {
                throw new RuntimeException("Test exception");
            }, () -> finalExecuted.set(true));
            assertThrows(Exception.class, failWithFinal::get);
            assertTrue(finalExecuted.get());

            assertTrue(executor.execute((Collection<Callable<Integer>>) null).isEmpty());
            assertTrue(executor.execute(new ArrayList<Callable<Integer>>()).isEmpty());
            List<Callable<Integer>> callables = Arrays.asList(() -> 1, () -> 2, () -> 3);
            List<ContinuableFuture<Integer>> futures = executor.execute(callables);
            assertEquals(3, futures.size());
            int sum = 0;
            for (ContinuableFuture<Integer> future : futures) {
                sum += future.get();
            }
            assertEquals(6, sum);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testExecuteWithRetry() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        try {
            AtomicInteger noRetry = new AtomicInteger();
            executor.executeWithRetry((Throwables.Runnable<Exception>) () -> noRetry.incrementAndGet(), 3, 10, e -> e instanceof RuntimeException).get();
            assertEquals(1, noRetry.get());

            AtomicInteger retries = new AtomicInteger();
            executor.executeWithRetry((Throwables.Runnable<Exception>) () -> {
                if (retries.incrementAndGet() < 3) {
                    throw new RuntimeException("Retry needed");
                }
            }, 2, 10, e -> e instanceof RuntimeException).get();
            assertEquals(3, retries.get());

            AtomicInteger maxExceeded = new AtomicInteger();
            ContinuableFuture<Void> alwaysFails = executor.executeWithRetry((Throwables.Runnable<Exception>) () -> {
                maxExceeded.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 2, 10, e -> e instanceof RuntimeException);
            assertThrows(Exception.class, alwaysFails::get);
            assertEquals(3, maxExceeded.get());

            AtomicInteger callableNoRetry = new AtomicInteger();
            assertEquals("Success", executor.executeWithRetry(() -> {
                callableNoRetry.incrementAndGet();
                return "Success";
            }, 3, 10, (result, exception) -> result == null).get());
            assertEquals(1, callableNoRetry.get());

            AtomicInteger nullRetry = new AtomicInteger();
            assertEquals("Success", executor.executeWithRetry(() -> {
                if (nullRetry.incrementAndGet() < 2) {
                    return null;
                }
                return "Success";
            }, 3, 10, (result, exception) -> result == null).get());
            assertEquals(2, nullRetry.get());

            AtomicInteger exceptionRetry = new AtomicInteger();
            assertEquals("Success", executor.executeWithRetry(() -> {
                if (exceptionRetry.incrementAndGet() < 3) {
                    throw new RuntimeException("Retry needed");
                }
                return "Success";
            }, 3, 10, (result, exception) -> exception != null).get());
            assertEquals(3, exceptionRetry.get());

            AtomicInteger callableMax = new AtomicInteger();
            ContinuableFuture<String> nullForever = executor.executeWithRetry(() -> {
                callableMax.incrementAndGet();
                return null;
            }, 2, 10, (result, exception) -> result == null);
            assertThrows(Exception.class, nullForever::get);
            assertEquals(3, callableMax.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testExecute_EdgeCase() throws Exception {
        AsyncExecutor executor = new AsyncExecutor(1, 1, 60L, TimeUnit.SECONDS);
        try {
            ContinuableFuture<Void> future = executor.execute(() -> {
                throw new IllegalStateException("primary");
            }, () -> {
                throw new IllegalArgumentException("cleanup");
            });
            try {
                future.get();
                fail("expected ExecutionException");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                assertTrue(cause instanceof IllegalStateException, "primary command failure must surface, got: " + cause);
                assertEquals("primary", cause.getMessage());
                boolean sawCleanup = false;
                for (Throwable s : cause.getSuppressed()) {
                    if (s instanceof IllegalArgumentException && "cleanup".equals(s.getMessage())) {
                        sawCleanup = true;
                    }
                }
                assertTrue(sawCleanup);
            }

            AssertionError primary = new AssertionError("primary");
            ContinuableFuture<Void> errorFuture = executor.execute(() -> {
                throw primary;
            }, () -> {
                throw new IllegalArgumentException("cleanup");
            });
            try {
                errorFuture.get();
                fail("expected ExecutionException");
            } catch (ExecutionException e) {
                assertSame(primary, e.getCause());
                assertEquals(1, primary.getSuppressed().length);
                assertEquals("cleanup", primary.getSuppressed()[0].getMessage());
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testConcurrentExecution() throws Exception {
        AsyncExecutor executor = new AsyncExecutor(4, 8, 60L, TimeUnit.SECONDS);
        try {
            AtomicInteger counter = new AtomicInteger();
            int taskCount = 10;
            CountDownLatch latch = new CountDownLatch(taskCount);
            List<ContinuableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.execute((Throwables.Runnable<Exception>) () -> {
                    counter.incrementAndGet();
                    latch.countDown();
                }));
            }
            latch.await(5, TimeUnit.SECONDS);
            assertEquals(taskCount, counter.get());
            for (ContinuableFuture<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testGetExecutor() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        try {
            Executor first = executor.getExecutor();
            assertSame(first, executor.getExecutor());

            AtomicReference<Executor> e1 = new AtomicReference<>();
            AtomicReference<Executor> e2 = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(2);
            Thread t1 = new Thread(() -> {
                e1.set(executor.getExecutor());
                latch.countDown();
            });
            Thread t2 = new Thread(() -> {
                e2.set(executor.getExecutor());
                latch.countDown();
            });
            t1.start();
            t2.start();
            latch.await(2, TimeUnit.SECONDS);
            assertSame(e1.get(), e2.get());
        } finally {
            executor.shutdown();
        }
        assertThrows(IllegalStateException.class, executor::getExecutor);

        ExecutorService javaExecutor = Executors.newFixedThreadPool(2);
        AsyncExecutor provided = new AsyncExecutor(javaExecutor);
        try {
            assertSame(javaExecutor, provided.getExecutor());
        } finally {
            provided.shutdown();
        }
    }

    @Test
    public void testGetExecutorNeverReturnsNullAcrossShutdownFieldTransition() throws Exception {
        Executor directExecutor = Runnable::run;
        AsyncExecutor executor = new AsyncExecutor(directExecutor);
        Field executorField = AsyncExecutor.class.getDeclaredField("executor");
        Field shutdownField = AsyncExecutor.class.getDeclaredField("isShutdown");
        executorField.setAccessible(true);
        shutdownField.setAccessible(true);
        shutdownField.setBoolean(executor, true);

        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<Throwable> writerFailure = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Thread transitionWriter = new Thread(() -> {
            started.countDown();
            try {
                while (!stop.get()) {
                    executorField.set(executor, null);
                    executorField.set(executor, directExecutor);
                }
            } catch (Throwable e) {
                writerFailure.set(e);
            }
        });
        transitionWriter.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        try {
            for (int i = 0; i < 100_000; i++) {
                try {
                    assertSame(directExecutor, executor.getExecutor());
                } catch (IllegalStateException e) {
                    // The null side of the transition is the already-shut-down state.
                }
            }
        } finally {
            stop.set(true);
            transitionWriter.join(TimeUnit.SECONDS.toMillis(2));
        }
        assertFalse(transitionWriter.isAlive());
        assertNull(writerFailure.get());
    }

    @Test
    public void testShutdown() throws Exception {
        AsyncExecutor uninitialized = new AsyncExecutor();
        uninitialized.shutdown();
        assertNotNull(uninitialized);

        AsyncExecutor executor = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute((Throwables.Runnable<Exception>) latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();

        AsyncExecutor timeout = new AsyncExecutor();
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        timeout.execute((Throwables.Runnable<Exception>) () -> {
            Thread.sleep(100);
            timeoutLatch.countDown();
        });
        timeout.shutdownAndAwait(2, TimeUnit.SECONDS);
        assertTrue(timeoutLatch.await(3, TimeUnit.SECONDS));

        AsyncExecutor zeroTimeout = new AsyncExecutor();
        zeroTimeout.execute((Throwables.Runnable<Exception>) () -> Thread.sleep(100));
        zeroTimeout.shutdownAndAwait(0, TimeUnit.SECONDS);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AsyncExecutor rejectsNull = new AsyncExecutor(executorService);
        try {
            assertThrows(IllegalArgumentException.class, () -> rejectsNull.shutdownAndAwait(1, null));
            assertFalse(executorService.isShutdown());
        } finally {
            rejectsNull.shutdown();
        }

        AsyncExecutor plain = new AsyncExecutor(Runnable::run);
        plain.shutdownAndAwait(1, TimeUnit.SECONDS);
        assertThrows(IllegalStateException.class, plain::getExecutor);

        AsyncExecutor afterInit = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger();
        afterInit.execute((Throwables.Runnable<Exception>) () -> counter.incrementAndGet()).get();
        afterInit.shutdown();
        awaitTerminated(afterInit);
        assertEquals(1, counter.get());
    }

    @Test
    public void testIsTerminated() throws Exception {
        AsyncExecutor uninitialized = new AsyncExecutor();
        assertFalse(uninitialized.isTerminated());
        uninitialized.shutdown();
        assertTrue(uninitialized.isTerminated());

        AsyncExecutor plain = new AsyncExecutor(Runnable::run);
        assertFalse(plain.isTerminated());
        plain.shutdown();
        assertTrue(plain.isTerminated());

        AsyncExecutor running = new AsyncExecutor();
        ContinuableFuture<Void> future = running.execute(() -> {
        });
        assertFalse(running.isTerminated());
        running.shutdown();
        future.get(2, TimeUnit.SECONDS);
        awaitTerminated(running);

        AsyncExecutor before = new AsyncExecutor();
        before.execute((Throwables.Runnable<Exception>) () -> Thread.sleep(50));
        assertFalse(before.isTerminated());
        before.shutdown();

        AsyncExecutor after = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        after.execute((Throwables.Runnable<Exception>) latch::countDown);
        latch.await(1, TimeUnit.SECONDS);
        after.shutdownAndAwait(2, TimeUnit.SECONDS);
        assertTrue(after.isTerminated());
    }

    @Test
    public void testShutdownDoesNotReportTerminatedWhileTaskRunning() throws Exception {
        AsyncExecutor executor = new AsyncExecutor(1, 1, 60L, TimeUnit.SECONDS);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
        });
        assertTrue(started.await(1, TimeUnit.SECONDS));
        try {
            executor.shutdown();
            assertFalse(executor.isTerminated());
            executor.shutdown();
            assertFalse(executor.isTerminated());
        } finally {
            release.countDown();
        }
        future.get(2, TimeUnit.SECONDS);
        awaitTerminated(executor);
    }

    @Test
    public void testShutdownAndAwaitAfterShutdownStillWaits() throws Exception {
        AsyncExecutor executor = new AsyncExecutor(1, 1, 60L, TimeUnit.SECONDS);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
        });
        assertTrue(started.await(1, TimeUnit.SECONDS));
        executor.shutdown();
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            release.countDown();
        });
        releaser.start();
        executor.shutdownAndAwait(3, TimeUnit.SECONDS);
        assertTrue(executor.isTerminated());
        future.get(2, TimeUnit.SECONDS);
        releaser.join(2000);
    }

    @Test
    public void reviewFixes20260908_terminatesAfterABorrowedPoolDiscardsAQueuedTask() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        AsyncExecutor executor = new AsyncExecutor(pool);
        AtomicBoolean queuedRan = new AtomicBoolean();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        ContinuableFuture<Void> queued = executor.execute((Throwables.Runnable<Exception>) () -> queuedRan.set(true));

        executor.shutdown();
        assertFalse(executor.isTerminated());

        // The owner of a borrowed pool may drain it. The wrapper submitted for the queued task is then thrown
        // away and can never release its own reservation, which used to pin isTerminated() to false for ever.
        pool.shutdownNow();
        release.countDown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        awaitTerminated(executor);
        assertFalse(queuedRan.get());
        assertTrue(queued.isDone());
        assertTrue(queued.isCancelled());
    }

    @Test
    public void reviewFixes20260908_terminatesAfterASaturationPolicyDiscardsATask() throws Exception {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardPolicy());
        AsyncExecutor executor = new AsyncExecutor(pool);
        AtomicBoolean discardedRan = new AtomicBoolean();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        executor.execute((Throwables.Runnable<Exception>) () -> {
        });
        // the single worker is busy and the one-slot queue is full: this submission is accepted and dropped
        ContinuableFuture<Void> discarded = executor.execute((Throwables.Runnable<Exception>) () -> discardedRan.set(true));

        release.countDown();
        executor.shutdown();
        // while the pool is alive nothing distinguishes a dropped task from one that has not started yet
        assertFalse(executor.isTerminated());

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        awaitTerminated(executor);
        assertFalse(discardedRan.get());
        assertTrue(discarded.isCancelled());
    }

    @Test
    public void reviewFixes20260908_reclaimsEveryOutstandingReservationNotJustOne() throws Exception {
        // The reclaim scan is short-circuited on one task's delegate, so it has to be shown that it still
        // releases and cancels ALL of them, not only the reservation it looked at.
        final ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardPolicy());
        final AsyncExecutor executor = new AsyncExecutor(pool);
        final AtomicInteger discardedRan = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        executor.execute((Throwables.Runnable<Exception>) () -> {
        }); // fills the one-slot queue

        final List<ContinuableFuture<Void>> discarded = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            discarded.add(executor.execute((Throwables.Runnable<Exception>) discardedRan::incrementAndGet));
        }

        release.countDown();
        executor.shutdown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // awaitTermination returning means every wrapper that ran has released its own reservation, so the only
        // ones left are the dropped tasks - and ONE call has to release them all, not one per call.
        assertTrue(executor.isTerminated());
        assertEquals(0, discardedRan.get());
        for (final ContinuableFuture<Void> f : discarded) {
            assertTrue(f.isCancelled(), "every dropped task must be cancelled, not just the first one seen");
        }
    }

    @Test
    public void reviewFixes20260908_aRunningTaskIsNeverCancelledByTheReclaimScan() throws Exception {
        // The reservation of a task that has already started is only ever released by the thread running it: the
        // reclaim has to lose the claim. Proven with a delegate that reports termination while it is still running
        // the task - the state the scan trusts - which no real pool does and no other test reaches.
        final ExecutorService inner = Executors.newSingleThreadExecutor();
        final ExecutorService alwaysTerminated = new java.util.concurrent.AbstractExecutorService() {
            @Override
            public void shutdown() {
                inner.shutdown();
            }

            @Override
            public List<Runnable> shutdownNow() {
                return inner.shutdownNow();
            }

            @Override
            public boolean isShutdown() {
                return true;
            }

            @Override
            public boolean isTerminated() {
                return true;
            }

            @Override
            public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
                return inner.awaitTermination(timeout, unit);
            }

            @Override
            public void execute(final Runnable command) {
                inner.execute(command);
            }
        };

        try {
            final AsyncExecutor executor = new AsyncExecutor(alwaysTerminated);
            final CountDownLatch started = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final ContinuableFuture<String> running = executor.execute((Callable<String>) () -> {
                started.countDown();
                release.await(5, TimeUnit.SECONDS);
                return "finished";
            });

            assertTrue(started.await(2, TimeUnit.SECONDS));

            for (int i = 0; i < 50; i++) {
                assertFalse(executor.isTerminated()); // each call runs the reclaim scan
            }

            release.countDown();
            assertEquals("finished", running.get(5, TimeUnit.SECONDS));
            assertFalse(running.isCancelled());

            executor.shutdown();
            awaitTerminated(executor);
        } finally {
            alwaysTerminated.shutdown();
            assertTrue(alwaysTerminated.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * R07-5: {@code shutdownAndAwait(long, TimeUnit)} declares its parameter as {@code timeUnit} but reported the
     * rejection under the name {@code unit}. {@code Fn.shutdown} had the identical defect fixed at r9506; this was
     * the only other site in the tree.
     */
    @Test
    public void reviewFixes20260908_shutdownAndAwaitNamesTheRejectedArgumentTimeUnit() {
        final AsyncExecutor executor = new AsyncExecutor();

        try {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> executor.shutdownAndAwait(1, null));

            assertTrue(ex.getMessage().contains("timeUnit"), "expected the message to name 'timeUnit' but got: " + ex.getMessage());
        } finally {
            executor.shutdown();
        }

        // A non-positive timeout still skips the check entirely, so a null unit is accepted there.
        final AsyncExecutor noWait = new AsyncExecutor();
        noWait.shutdownAndAwait(0, null);
        assertTrue(noWait.isTerminated());
    }

    @Test
    public void testToString() {
        AsyncExecutor def = new AsyncExecutor();
        assertTrue(def.toString().contains("coreThreadPoolSize"));
        assertTrue(def.toString().contains("maxThreadPoolSize"));
        assertTrue(def.toString().contains("keepAliveTime"));
        def.shutdown();

        AsyncExecutor custom = new AsyncExecutor(5, 10, 90L, TimeUnit.SECONDS);
        assertTrue(custom.toString().contains("coreThreadPoolSize: 5"));
        assertTrue(custom.toString().contains("maxThreadPoolSize: 10"));
        assertTrue(custom.toString().contains("keepAliveTime: 90000ms"));
        custom.getExecutor();
        assertTrue(custom.toString().contains("activeCount"));
        custom.shutdown();

        AsyncExecutor before = new AsyncExecutor(3, 6, 45L, TimeUnit.SECONDS);
        assertTrue(before.toString().contains("coreThreadPoolSize: 3"));
        before.shutdown();
    }

    /**
     * G33-001: every submit path propagates {@link RejectedExecutionException} from the delegate, which the
     * javadoc now documents. Contract pin for that tag - it holds before and after the doc fix.
     */
    @Test
    public void testExecuteRejectedExecutionExceptionPropagates() throws Exception {
        final ThreadPoolExecutor bounded = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), r -> {
            final Thread t = new Thread(r, "async-ree-pin");
            t.setDaemon(true);
            return t;
        });
        final AsyncExecutor executor = new AsyncExecutor(bounded);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        try {
            final ContinuableFuture<Void> running = executor.execute(() -> {
                started.countDown();
                release.await(10, TimeUnit.SECONDS);
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));

            final ContinuableFuture<Void> queued = executor.execute(() -> {
                release.await(10, TimeUnit.SECONDS);
            });

            // the single worker is busy and the one-slot queue now holds the second task, so AbortPolicy rejects
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {
                started.countDown();
            }));

            release.countDown();
            assertNull(running.get(10, TimeUnit.SECONDS));
            assertNull(queued.get(10, TimeUnit.SECONDS));

            executor.shutdown();
            awaitTerminated(executor); // the rejected submission left no reservation behind
        } finally {
            release.countDown();
            bounded.shutdownNow();
            assertTrue(bounded.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * G33-002: getExecutor() published the lazily created pool before calling Runtime.addShutdownHook, so when
     * that call fails - it throws once JVM shutdown has begun - the first submit failed while the instance kept
     * a live, hook-less pool, and an identical second submit then silently succeeded. Reproduced in a child JVM
     * because the failure needs a real JVM shutdown.
     */
    @Test
    public void testExecuteIsNotLeftHalfInitializedWhenTheShutdownHookCannotBeRegistered() throws Exception {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        // java.class.path is what a Surefire fork exposes: with the default useSystemClassLoader=true that is
        // the manifest-only booter jar, whose manifest Class-Path the child JVM expands for us. A run that set
        // useSystemClassLoader=false would hide the test classpath behind an isolated loader and the child would
        // die with NoClassDefFoundError - which is still diagnosable here, because every assertion below embeds
        // the child's own output.
        final ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", System.getProperty("java.class.path"),
                ShutdownHookSubmitProbe.class.getName());
        builder.redirectErrorStream(true);

        // The merged output must go to a file, not a pipe. Nothing drains a pipe until waitFor() has returned,
        // so a child that out-writes the pipe buffer would block in write(), never exit, and this test would
        // report the bogus "did not exit within 90s" below instead of the real PROBE mismatch.
        final File outputFile = File.createTempFile("async-shutdown-hook-probe-", ".log");
        builder.redirectOutput(outputFile);

        final String output;

        try {
            final Process process = builder.start();

            try {
                if (!process.waitFor(90, TimeUnit.SECONDS)) {
                    fail("the shutdown-hook probe JVM did not exit within 90s");
                }
            } finally {
                process.destroyForcibly();
            }

            output = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(outputFile.toPath());
        }

        String probeLine = null;

        for (final String line : output.split("\\R")) {
            if (line.startsWith("PROBE ")) {
                probeLine = line.trim();
            }
        }

        assertNotNull(probeLine, "no PROBE line in the child output:\n" + output);
        assertEquals("PROBE first=OK second=OK", probeLine, "child output was:\n" + output);
    }

    /**
     * Creates the lazy pool from inside a JVM shutdown hook, where addShutdownHook must fail.
     */
    public static final class ShutdownHookSubmitProbe {

        public static void main(final String[] args) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                final AsyncExecutor asyncExecutor = new AsyncExecutor(1, 1, 1L, TimeUnit.SECONDS);
                final Throwables.Runnable<RuntimeException> task = () -> {
                };

                System.out.println("PROBE first=" + submit(asyncExecutor, task) + " second=" + submit(asyncExecutor, task));
            }, "async-shutdown-hook-probe"));
        }

        private static String submit(final AsyncExecutor asyncExecutor, final Throwables.Runnable<RuntimeException> task) {
            try {
                asyncExecutor.execute(task);

                return "OK";
            } catch (final IllegalStateException e) {
                return "ISE";
            } catch (final Throwable e) {
                return e.getClass().getSimpleName();
            }
        }
    }

    /**
     * G33-003: an interrupt that arrives while shutdownAndAwait is waiting for an owned pool to terminate
     * returns early and silently - the javadoc no longer promises a warning for that case.
     */
    @Test
    public void testShutdownAndAwaitReturnsEarlySilentlyWhenInterruptedWaitingForTheOwnedPool() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(1, 1, 1L, TimeUnit.SECONDS);
        final ExecutorService pool = (ExecutorService) executor.getExecutor();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        try {
            // submitted straight to the pool, so this instance holds no reservation at all and the only wait
            // left is pool termination - the one interrupt path that does not log
            pool.execute(() -> {
                started.countDown();

                try {
                    release.await(30, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));

            Thread.currentThread().interrupt();
            final long startedAt = System.nanoTime();
            executor.shutdownAndAwait(30, TimeUnit.SECONDS);
            final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertTrue(Thread.interrupted(), "the interrupt status must be restored");
            assertTrue(elapsedMillis < 5_000, "expected an early return but it waited " + elapsedMillis + "ms");
            assertFalse(executor.isTerminated()); // the directly submitted task is still running
        } finally {
            release.countDown();
            Thread.interrupted();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * G33-004: both batch overloads submit the commands preceding a {@code null} element - those commands run -
     * and then throw IllegalArgumentException, so their futures are lost. Pins the documented behaviour.
     */
    @Test
    public void testExecuteBatchWithNullElementSubmitsThePrecedingCommands() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(2, 2, 1L, TimeUnit.SECONDS);
        final CountDownLatch ranRunnable = new CountDownLatch(1);
        final CountDownLatch ranCallable = new CountDownLatch(1);

        try {
            final List<Throwables.Runnable<? extends Exception>> runnables = new ArrayList<>();
            runnables.add(() -> ranRunnable.countDown());
            runnables.add(null);

            final IllegalArgumentException runnableEx = assertThrows(IllegalArgumentException.class, () -> executor.execute(runnables));
            assertTrue(runnableEx.getMessage().contains("command"), runnableEx.getMessage());
            assertTrue(ranRunnable.await(10, TimeUnit.SECONDS), "the first command was submitted and ran, yet its future was lost");
            assertEquals(0, ranRunnable.getCount());

            final List<Callable<String>> callables = new ArrayList<>();
            callables.add(() -> {
                ranCallable.countDown();
                return "first";
            });
            callables.add(null);

            final IllegalArgumentException callableEx = assertThrows(IllegalArgumentException.class, () -> executor.execute(callables));
            assertTrue(callableEx.getMessage().contains("command"), callableEx.getMessage());
            assertTrue(ranCallable.await(10, TimeUnit.SECONDS), "the first command was submitted and ran, yet its future was lost");
            assertEquals(0, ranCallable.getCount());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * The batch {@code @throws IllegalArgumentException} promises only that the preceding commands were
     * <i>submitted</i>: each may still be running when the exception surfaces, or may already have
     * completed. Both halves are gated on a latch, so neither assertion is a race.
     */
    @Test
    public void testExecuteBatchNullElementPromisesSubmissionNotThatTheyAreStillRunning() throws Exception {
        final AsyncExecutor executor = new AsyncExecutor(2, 2, 1L, TimeUnit.SECONDS);

        try {
            // (1) already completed: the null element is only handed to the loop after the first
            // command's body has finished, so at throw time nothing of it is running.
            final CountDownLatch bodyFinished = new CountDownLatch(1);
            final AtomicBoolean completed = new AtomicBoolean(false);
            final AtomicBoolean completedAtThrow = new AtomicBoolean(false);

            final List<Throwables.Runnable<? extends Exception>> gated = new AbstractList<>() {
                @Override
                public int size() {
                    return 2;
                }

                @Override
                public Throwables.Runnable<? extends Exception> get(final int index) {
                    if (index == 0) {
                        return () -> {
                            completed.set(true);
                            bodyFinished.countDown();
                        };
                    }

                    try {
                        completedAtThrow.set(bodyFinished.await(10, TimeUnit.SECONDS) && completed.get());
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    return null;
                }
            };

            assertThrows(IllegalArgumentException.class, () -> executor.execute(gated));
            assertTrue(completedAtThrow.get(), "the preceding command had already completed, so it was not 'still running'");

            // (2) still running: the first command parks inside its body until this thread releases it,
            // so it is provably mid-flight when the IllegalArgumentException is caught.
            final CountDownLatch started = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final AtomicBoolean finished = new AtomicBoolean(false);

            final List<Callable<String>> blocking = new AbstractList<>() {
                @Override
                public int size() {
                    return 2;
                }

                @Override
                public Callable<String> get(final int index) {
                    if (index == 0) {
                        return () -> {
                            started.countDown();
                            release.await(10, TimeUnit.SECONDS);
                            finished.set(true);
                            return "first";
                        };
                    }

                    try {
                        started.await(10, TimeUnit.SECONDS);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    return null;
                }
            };

            assertThrows(IllegalArgumentException.class, () -> executor.execute(blocking));
            assertFalse(finished.get(), "the preceding command was still inside its body at throw time");

            release.countDown();

            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!finished.get() && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }

            assertTrue(finished.get(), "the preceding command was submitted, so it must run to completion");
        } finally {
            executor.shutdown();
        }
    }
}
