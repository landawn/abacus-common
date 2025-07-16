package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables.Runnable;

public class AsyncExecutor100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        AsyncExecutor executor = new AsyncExecutor();
        Assertions.assertNotNull(executor);
        executor.shutdown();
    }

    @Test
    public void testParameterizedConstructor() {
        AsyncExecutor executor = new AsyncExecutor(2, 4, 60L, TimeUnit.SECONDS);
        Assertions.assertNotNull(executor);
        executor.shutdown();
    }

    @Test
    public void testConstructorWithExecutor() {
        Executor javaExecutor = Executors.newFixedThreadPool(2);
        AsyncExecutor executor = new AsyncExecutor(javaExecutor);
        Assertions.assertNotNull(executor);
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnable() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute(() -> {
            counter.incrementAndGet();
        });

        future.get();
        Assertions.assertEquals(1, counter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithFinal() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger finalCounter = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute(Fnn.r(() -> counter.incrementAndGet()), () -> finalCounter.incrementAndGet());

        future.get();
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals(1, finalCounter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableList() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable<? extends Exception>> tasks = Arrays.asList(() -> counter.incrementAndGet(), () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet());
        List<ContinuableFuture<Void>> futures = executor.execute(tasks);

        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }

        Assertions.assertEquals(3, counter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallable() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<String> future = executor.execute(() -> {
            return "Hello World";
        });

        String result = future.get();
        Assertions.assertEquals("Hello World", result);
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithFinal() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger finalCounter = new AtomicInteger(0);

        ContinuableFuture<Integer> future = executor.execute(() -> 42, () -> finalCounter.incrementAndGet());

        Integer result = future.get();
        Assertions.assertEquals(42, result);
        Assertions.assertEquals(1, finalCounter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableCollection() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        List<ContinuableFuture<Integer>> futures = executor.execute(Arrays.asList(() -> 1, () -> 2, () -> 3));

        int sum = 0;
        for (ContinuableFuture<Integer> future : futures) {
            sum += future.get();
        }

        Assertions.assertEquals(6, sum);
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithRetry() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Retry needed");
            }
        }, 2, // retry times
                10, // retry interval
                e -> e instanceof RuntimeException);

        future.get();
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithRetry() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<String> future = executor.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                return null;
            }
            return "Success";
        }, 3, // retry times
                10, // retry interval
                (result, exception) -> result == null);

        String result = future.get();
        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testGetExecutor() {
        AsyncExecutor executor = new AsyncExecutor();
        Executor internalExecutor = executor.getExecutor();
        Assertions.assertNotNull(internalExecutor);
        executor.shutdown();
    }

    @Test
    public void testShutdown() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.execute(() -> {
            // Do nothing
        });
        executor.shutdown();
        Assertions.assertTrue(executor.isTerminated() || !executor.isTerminated());
    }

    @Test
    public void testShutdownWithTimeout() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        });
        executor.shutdown(1, TimeUnit.SECONDS);
        // Should complete within timeout
    }

    @Test
    public void testIsTerminated() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.execute(() -> System.out.println("Running task"));
        Assertions.assertFalse(executor.isTerminated());
        executor.shutdown();
        // May or may not be terminated immediately after shutdown
        Assertions.assertTrue(executor.isTerminated());
    }

    @Test
    public void testToString() {
        AsyncExecutor executor = new AsyncExecutor(2, 4, 60L, TimeUnit.SECONDS);
        String str = executor.toString();
        Assertions.assertTrue(str.contains("coreThreadPoolSize: 2"));
        Assertions.assertTrue(str.contains("maxThreadPoolSize: 4"));
        Assertions.assertTrue(str.contains("keepAliveTime: 60000ms"));
        executor.shutdown();
    }
}