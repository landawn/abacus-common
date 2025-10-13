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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MoreExecutors100Test extends TestBase {

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

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MoreExecutors.newThread("test", null);
        });
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
}
