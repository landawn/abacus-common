package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MoreExecutorsTest extends TestBase {

    @Test
    public void testNegativeTerminationTimeoutRejectedBeforeExecutorMutation() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        ThreadFactory originalFactory = executor.getThreadFactory();
        ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(1);
        ThreadFactory originalScheduledFactory = scheduled.getThreadFactory();
        try {
            assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingExecutorService(executor, -1, TimeUnit.MILLISECONDS));
            assertSame(originalFactory, executor.getThreadFactory());
            assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingScheduledExecutorService(scheduled, -1, TimeUnit.MILLISECONDS));
            assertSame(originalScheduledFactory, scheduled.getThreadFactory());
        } finally {
            executor.shutdownNow();
            scheduled.shutdownNow();
        }
    }

    @Test
    public void testGetExitingExecutorService() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);
        Assertions.assertNotNull(exitingService);

        AtomicBoolean taskRan = new AtomicBoolean(false);
        exitingService.submit(() -> taskRan.set(true));

        Thread.sleep(100);
        Assertions.assertTrue(taskRan.get());

        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExitingExecutorServiceWithTimeout() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor, 30, TimeUnit.SECONDS);
        Assertions.assertNotNull(exitingService);

        Future<String> future = exitingService.submit(() -> "test result");
        Assertions.assertEquals("test result", future.get());

        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testDaemonThreads() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);

        AtomicBoolean isDaemon = new AtomicBoolean(false);
        exitingService.submit(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
        }).get();

        Assertions.assertTrue(isDaemon.get());

        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testDaemonThreadFactoryPreservesNullFromDelegate() {
        final ThreadFactory decliningFactory = runnable -> null;
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), decliningFactory);
        final ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);

        try {
            Assertions.assertNull(executor.getThreadFactory().newThread(() -> {
            }));
        } finally {
            exitingService.shutdownNow();
        }
    }

    @Test
    public void testExecutorServiceMethods() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);

        Assertions.assertFalse(exitingService.isShutdown());

        Assertions.assertFalse(exitingService.isTerminated());

        CountDownLatch latch = new CountDownLatch(1);
        exitingService.execute(latch::countDown);
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));

        Future<?> runnableFuture = exitingService.submit(() -> {
        });
        runnableFuture.get(1, TimeUnit.SECONDS);
        Assertions.assertTrue(runnableFuture.isDone());

        Future<Integer> callableFuture = exitingService.submit(() -> 42);
        Assertions.assertEquals(42, callableFuture.get());

        exitingService.shutdown();
        Assertions.assertTrue(exitingService.isShutdown());

        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
        Assertions.assertTrue(exitingService.isTerminated());
    }

    @Test
    public void testGetExitingScheduledExecutorService() throws Exception {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);

        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler);
        Assertions.assertNotNull(exitingScheduler);

        AtomicBoolean taskRan = new AtomicBoolean(false);
        ScheduledFuture<?> scheduledFuture = exitingScheduler.schedule(() -> taskRan.set(true), 50, TimeUnit.MILLISECONDS);

        scheduledFuture.get();
        Assertions.assertTrue(taskRan.get());

        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExitingScheduledExecutorServiceWithTimeout() throws Exception {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler, 10, TimeUnit.SECONDS);
        Assertions.assertNotNull(exitingScheduler);

        AtomicInteger counter = new AtomicInteger(0);
        ScheduledFuture<?> periodicFuture = exitingScheduler.scheduleAtFixedRate(counter::incrementAndGet, 0, 50, TimeUnit.MILLISECONDS);

        Thread.sleep(200);
        periodicFuture.cancel(true);

        Assertions.assertTrue(counter.get() >= 3);

        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testScheduledExecutorServiceMethods() throws Exception {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);
        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler);

        AtomicBoolean ran1 = new AtomicBoolean(false);
        ScheduledFuture<?> future1 = exitingScheduler.schedule(() -> ran1.set(true), 50, TimeUnit.MILLISECONDS);
        future1.get();
        Assertions.assertTrue(ran1.get());

        ScheduledFuture<String> future2 = exitingScheduler.schedule(() -> "scheduled result", 50, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("scheduled result", future2.get());

        AtomicInteger counter = new AtomicInteger(0);
        ScheduledFuture<?> future3 = exitingScheduler.scheduleWithFixedDelay(counter::incrementAndGet, 0, 50, TimeUnit.MILLISECONDS);

        Thread.sleep(150);
        future3.cancel(true);
        Assertions.assertTrue(counter.get() >= 2);

        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testAddDelayedShutdownHook() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        MoreExecutors.addDelayedShutdownHook(executor, 500, TimeUnit.MILLISECONDS);

        Future<String> future = executor.submit(() -> "test");
        Assertions.assertEquals("test", future.get());

        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testAddDelayedShutdownHookInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.addDelayedShutdownHook(null, 1, TimeUnit.SECONDS);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.addDelayedShutdownHook(executor, 1, null);
        });

        executor.shutdown();
    }

    @Test
    public void testAddShutdownHook() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        Thread hook = new Thread(() -> hookCalled.set(true));

        MoreExecutors.addShutdownHook(hook);

        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
        }
        assertNotNull(hook);
    }

    @Test
    public void testNewThread() {
        String threadName = "TestThread";
        AtomicBoolean taskRan = new AtomicBoolean(false);

        Thread thread = MoreExecutors.newThread(threadName, () -> taskRan.set(true));

        Assertions.assertNotNull(thread);
        Assertions.assertEquals(threadName, thread.getName());

        thread.start();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Assertions.assertTrue(taskRan.get());
    }

    @Test
    public void testNewThreadInvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.newThread(null, () -> {
            });
        });

        // G18-73 (2026-09-08): re-pointed. This used to assert that a null runnable is accepted, which pinned the
        // dropped N.checkArgNotNull(runnable) - the call built a thread that silently does nothing when started.
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.newThread("test", null);
        });
    }

    // G18-73 (2026-09-08): a conversion that fails after the thread factory was swapped must put the caller's
    // factory back, so that a failed call never leaves a daemonised pool with no hook to drain it.
    @Test
    public void fixG18_failedConversionRestoresTheOriginalThreadFactory() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        try {
            final ThreadFactory originalFactory = executor.getThreadFactory();
            final ExecutorService service = MoreExecutors.getExitingExecutorService(executor, 1, TimeUnit.SECONDS);
            final ThreadFactory installedFactory = executor.getThreadFactory();

            assertNotNull(service);
            Assertions.assertNotSame(originalFactory, installedFactory, "the conversion must install its own daemon factory");

            // The rollback the conversion runs when the shutdown hook cannot be registered.
            MoreExecutors.restoreThreadFactory(executor, installedFactory, originalFactory);
            assertSame(originalFactory, executor.getThreadFactory());

            // Nothing to undo: the factory is no longer the one this class installed, so the rollback is a no-op.
            MoreExecutors.restoreThreadFactory(executor, installedFactory, originalFactory);
            assertSame(originalFactory, executor.getThreadFactory());

            // A factory a third party installed while the conversion was running is the more recent intent: keep it.
            final ThreadFactory foreignFactory = Executors.defaultThreadFactory();
            executor.setThreadFactory(foreignFactory);
            MoreExecutors.restoreThreadFactory(executor, installedFactory, originalFactory);
            assertSame(foreignFactory, executor.getThreadFactory());
        } finally {
            executor.shutdownNow();
        }
    }

    // G18-73 (2026-09-08): a failing rollback must not replace the exception that triggered it.
    @Test
    public void fixG18_failedRollbackIsSwallowed() {
        final ThreadFactory[] installed = new ThreadFactory[1];
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
            @Override
            public void setThreadFactory(final ThreadFactory threadFactory) {
                if (installed[0] != null && threadFactory != installed[0]) {
                    throw new IllegalStateException("refusing to restore");
                }

                super.setThreadFactory(threadFactory);
            }
        };

        try {
            final ThreadFactory originalFactory = executor.getThreadFactory();
            MoreExecutors.getExitingExecutorService(executor, 1, TimeUnit.SECONDS);
            installed[0] = executor.getThreadFactory();

            Assertions.assertDoesNotThrow(() -> MoreExecutors.restoreThreadFactory(executor, installed[0], originalFactory));
            assertSame(installed[0], executor.getThreadFactory());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testInvalidExitingArgumentsDoNotMutateExecutors() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        final java.util.concurrent.ThreadFactory originalFactory = executor.getThreadFactory();

        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingExecutorService(executor, 1, null));
            Assertions.assertSame(originalFactory, executor.getThreadFactory());
            Assertions.assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingExecutorService(null, 1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        final java.util.concurrent.ThreadFactory originalScheduledFactory = scheduledExecutor.getThreadFactory();

        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingScheduledExecutorService(scheduledExecutor, 1, null));
            Assertions.assertSame(originalScheduledFactory, scheduledExecutor.getThreadFactory());
            Assertions.assertThrows(IllegalArgumentException.class, () -> MoreExecutors.getExitingScheduledExecutorService(null, 1, TimeUnit.SECONDS));
        } finally {
            scheduledExecutor.shutdownNow();
        }
    }

    @Test
    public void testDaemonConversionOnlyAffectsThreadsCreatedAfterwards() {
        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(20), () -> {
            // corePoolSize 2 with an unbounded queue: ThreadPoolExecutor.execute() adds a fresh worker while the
            // pool is below core size, so the second submit is guaranteed to build a thread with the new factory.
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 600L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
            final java.util.concurrent.atomic.AtomicReference<Thread> beforeThread = new java.util.concurrent.atomic.AtomicReference<>();
            final java.util.concurrent.atomic.AtomicReference<Thread> afterThread = new java.util.concurrent.atomic.AtomicReference<>();

            try {
                final CountDownLatch startedBefore = new CountDownLatch(1);
                executor.execute(() -> {
                    beforeThread.set(Thread.currentThread());
                    startedBefore.countDown();
                });
                Assertions.assertTrue(startedBefore.await(10, TimeUnit.SECONDS));

                final ExecutorService exiting = MoreExecutors.getExitingExecutorService(executor, 1, TimeUnit.MILLISECONDS);
                assertNotNull(exiting);

                final CountDownLatch startedAfter = new CountDownLatch(1);
                exiting.execute(() -> {
                    afterThread.set(Thread.currentThread());
                    startedAfter.countDown();
                });
                Assertions.assertTrue(startedAfter.await(10, TimeUnit.SECONDS));

                Assertions.assertNotSame(beforeThread.get(), afterThread.get());
                Assertions.assertFalse(beforeThread.get().isDaemon(), "a worker that existed before the conversion stays non-daemon");
                Assertions.assertTrue(afterThread.get().isDaemon(), "a worker created after the conversion is a daemon");
            } finally {
                executor.shutdownNow();
            }
        });
    }
}
