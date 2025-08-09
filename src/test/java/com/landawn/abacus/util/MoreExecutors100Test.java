package com.landawn.abacus.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MoreExecutors100Test extends TestBase {

    @Test
    public void testGetExitingExecutorService() throws Exception {
        // Test with default timeout
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);
        Assertions.assertNotNull(exitingService);

        // Test that tasks can be submitted
        AtomicBoolean taskRan = new AtomicBoolean(false);
        exitingService.submit(() -> taskRan.set(true));

        // Wait for task to complete
        Thread.sleep(100);
        Assertions.assertTrue(taskRan.get());

        // Shutdown for cleanup
        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExitingExecutorServiceWithTimeout() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        // Test with custom timeout
        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor, 30, TimeUnit.SECONDS);
        Assertions.assertNotNull(exitingService);

        // Submit a task
        Future<String> future = exitingService.submit(() -> "test result");
        Assertions.assertEquals("test result", future.get());

        // Cleanup
        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExitingScheduledExecutorService() throws Exception {
        // Test with default timeout
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);

        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler);
        Assertions.assertNotNull(exitingScheduler);

        // Test scheduling
        AtomicBoolean taskRan = new AtomicBoolean(false);
        ScheduledFuture<?> scheduledFuture = exitingScheduler.schedule(() -> taskRan.set(true), 50, TimeUnit.MILLISECONDS);

        // Wait for task to complete
        scheduledFuture.get();
        Assertions.assertTrue(taskRan.get());

        // Cleanup
        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testGetExitingScheduledExecutorServiceWithTimeout() throws Exception {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

        // Test with custom timeout
        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler, 10, TimeUnit.SECONDS);
        Assertions.assertNotNull(exitingScheduler);

        // Test periodic scheduling
        AtomicInteger counter = new AtomicInteger(0);
        ScheduledFuture<?> periodicFuture = exitingScheduler.scheduleAtFixedRate(counter::incrementAndGet, 0, 50, TimeUnit.MILLISECONDS);

        // Let it run a few times
        Thread.sleep(200);
        periodicFuture.cancel(true);

        Assertions.assertTrue(counter.get() >= 3);

        // Cleanup
        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testAddDelayedShutdownHook() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Add shutdown hook
        MoreExecutors.addDelayedShutdownHook(executor, 500, TimeUnit.MILLISECONDS);

        // Submit a task to verify executor is working
        Future<String> future = executor.submit(() -> "test");
        Assertions.assertEquals("test", future.get());

        // Note: We can't easily test the actual shutdown hook behavior in a unit test
        // as it requires JVM shutdown. The hook is registered but won't execute during test.

        // Cleanup
        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testAddDelayedShutdownHookInvalidInput() {
        // Test null service
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.addDelayedShutdownHook(null, 1, TimeUnit.SECONDS);
        });

        // Test null time unit
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.addDelayedShutdownHook(executor, 1, null);
        });

        // Cleanup
        executor.shutdown();
    }

    @Test
    public void testDaemonThreads() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        // Get exiting executor service
        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);

        // Submit a task and verify it runs on a daemon thread
        AtomicBoolean isDaemon = new AtomicBoolean(false);
        exitingService.submit(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
        }).get();

        Assertions.assertTrue(isDaemon.get());

        // Cleanup
        exitingService.shutdown();
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testNewThread() {
        String threadName = "TestThread";
        AtomicBoolean taskRan = new AtomicBoolean(false);

        Thread thread = MoreExecutors.newThread(threadName, () -> taskRan.set(true));

        Assertions.assertNotNull(thread);
        Assertions.assertEquals(threadName, thread.getName());

        // Run the thread
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
        // Test null name
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.newThread(null, () -> {
            });
        });

        // Test null runnable
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.newThread("test", null);
        });
    }

    @Test
    public void testAddShutdownHook() {
        // Test that we can add a shutdown hook
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        Thread hook = new Thread(() -> hookCalled.set(true));

        // This should not throw
        MoreExecutors.addShutdownHook(hook);

        // Note: We can't test that the hook actually runs without shutting down the JVM
        // The hook is registered but won't execute during the test

        // Try to remove the hook so it doesn't interfere with other tests
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            // Might fail if shutdown is already in progress, which is fine
        }
    }

    @Test
    public void testExecutorServiceMethods() throws Exception {
        // Test that the wrapped executor service properly delegates methods
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        ExecutorService exitingService = MoreExecutors.getExitingExecutorService(executor);

        // Test isShutdown
        Assertions.assertFalse(exitingService.isShutdown());

        // Test isTerminated
        Assertions.assertFalse(exitingService.isTerminated());

        // Test execute
        CountDownLatch latch = new CountDownLatch(1);
        exitingService.execute(latch::countDown);
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));

        // Test submit with Runnable
        Future<?> runnableFuture = exitingService.submit(() -> {
        });
        runnableFuture.get(1, TimeUnit.SECONDS);
        Assertions.assertTrue(runnableFuture.isDone());

        // Test submit with Callable
        Future<Integer> callableFuture = exitingService.submit(() -> 42);
        Assertions.assertEquals(42, callableFuture.get());

        // Test shutdown
        exitingService.shutdown();
        Assertions.assertTrue(exitingService.isShutdown());

        // Test awaitTermination
        Assertions.assertTrue(exitingService.awaitTermination(1, TimeUnit.SECONDS));
        Assertions.assertTrue(exitingService.isTerminated());
    }

    @Test
    public void testScheduledExecutorServiceMethods() throws Exception {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2);
        ScheduledExecutorService exitingScheduler = MoreExecutors.getExitingScheduledExecutorService(scheduler);

        // Test schedule with Runnable
        AtomicBoolean ran1 = new AtomicBoolean(false);
        ScheduledFuture<?> future1 = exitingScheduler.schedule(() -> ran1.set(true), 50, TimeUnit.MILLISECONDS);
        future1.get();
        Assertions.assertTrue(ran1.get());

        // Test schedule with Callable
        ScheduledFuture<String> future2 = exitingScheduler.schedule(() -> "scheduled result", 50, TimeUnit.MILLISECONDS);
        Assertions.assertEquals("scheduled result", future2.get());

        // Test scheduleWithFixedDelay
        AtomicInteger counter = new AtomicInteger(0);
        ScheduledFuture<?> future3 = exitingScheduler.scheduleWithFixedDelay(counter::incrementAndGet, 0, 50, TimeUnit.MILLISECONDS);

        Thread.sleep(150);
        future3.cancel(true);
        Assertions.assertTrue(counter.get() >= 2);

        // Cleanup
        exitingScheduler.shutdown();
        Assertions.assertTrue(exitingScheduler.awaitTermination(1, TimeUnit.SECONDS));
    }
}