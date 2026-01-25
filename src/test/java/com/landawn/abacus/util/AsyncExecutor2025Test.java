package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AsyncExecutor2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        AsyncExecutor executor = new AsyncExecutor();
        Assertions.assertNotNull(executor);
        Assertions.assertNotNull(executor.getExecutor());
        executor.shutdown();
    }

    @Test
    public void testParameterizedConstructor() {
        AsyncExecutor executor = new AsyncExecutor(4, 8, 120L, TimeUnit.SECONDS);
        Assertions.assertNotNull(executor);
        String str = executor.toString();
        Assertions.assertTrue(str.contains("coreThreadPoolSize: 4"));
        Assertions.assertTrue(str.contains("maxThreadPoolSize: 8"));
        executor.shutdown();
    }

    @Test
    public void testParameterizedConstructorNegativeCorePoolSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AsyncExecutor(-1, 4, 60L, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testParameterizedConstructorNegativeMaxPoolSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AsyncExecutor(2, -1, 60L, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testParameterizedConstructorNegativeKeepAliveTime() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AsyncExecutor(2, 4, -1L, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testParameterizedConstructorNullTimeUnit() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AsyncExecutor(2, 4, 60L, null);
        });
    }

    @Test
    public void testParameterizedConstructorMaxLessThanCore() {
        AsyncExecutor executor = new AsyncExecutor(8, 4, 60L, TimeUnit.SECONDS);
        Assertions.assertNotNull(executor);
        String str = executor.toString();
        Assertions.assertTrue(str.contains("maxThreadPoolSize: 8"));
        executor.shutdown();
    }

    @Test
    public void testConstructorWithThreadPoolExecutor() {
        ThreadPoolExecutor javaExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        AsyncExecutor executor = new AsyncExecutor(javaExecutor);
        Assertions.assertNotNull(executor);
        Assertions.assertSame(javaExecutor, executor.getExecutor());
        executor.shutdown();
    }

    @Test
    public void testConstructorWithPlainExecutor() {
        Executor javaExecutor = new Executor() {
            @Override
            public void execute(java.lang.Runnable command) {
                command.run();
            }
        };
        AsyncExecutor executor = new AsyncExecutor(javaExecutor);
        Assertions.assertNotNull(executor);
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnable() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            counter.incrementAndGet();
        });

        future.get();
        Assertions.assertEquals(1, counter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithException() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            throw new RuntimeException("Test exception");
        });

        Assertions.assertThrows(Exception.class, () -> future.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithFinalAction() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger finalCounter = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> counter.incrementAndGet(),
                () -> finalCounter.incrementAndGet());

        future.get();
        Assertions.assertEquals(1, counter.get());
        Assertions.assertEquals(1, finalCounter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithFinalActionOnException() {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicBoolean finalExecuted = new AtomicBoolean(false);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            throw new RuntimeException("Test exception");
        }, () -> finalExecuted.set(true));

        Assertions.assertThrows(Exception.class, () -> future.get());
        Assertions.assertTrue(finalExecuted.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableListEmpty() {
        AsyncExecutor executor = new AsyncExecutor();
        List<Throwables.Runnable<? extends Exception>> tasks = new ArrayList<>();
        List<ContinuableFuture<Void>> futures = executor.execute(tasks);
        Assertions.assertTrue(futures.isEmpty());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableListNull() {
        AsyncExecutor executor = new AsyncExecutor();
        List<ContinuableFuture<Void>> futures = executor.execute((List<Throwables.Runnable<? extends Exception>>) null);
        Assertions.assertTrue(futures.isEmpty());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableList() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger counter = new AtomicInteger(0);

        List<Throwables.Runnable<? extends Exception>> tasks = Arrays.asList(() -> counter.incrementAndGet(), () -> counter.incrementAndGet(),
                () -> counter.incrementAndGet());
        List<ContinuableFuture<Void>> futures = executor.execute(tasks);

        Assertions.assertEquals(3, futures.size());

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
    public void testExecuteCallableWithException() {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<String> future = executor.execute(() -> {
            throw new RuntimeException("Test exception");
        });

        Assertions.assertThrows(Exception.class, () -> future.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableReturningNull() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<String> future = executor.execute(() -> {
            return null;
        });

        String result = future.get();
        Assertions.assertNull(result);
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithFinalAction() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger finalCounter = new AtomicInteger(0);

        ContinuableFuture<Integer> future = executor.execute(() -> 42, () -> finalCounter.incrementAndGet());

        Integer result = future.get();
        Assertions.assertEquals(42, result);
        Assertions.assertEquals(1, finalCounter.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithFinalActionOnException() {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicBoolean finalExecuted = new AtomicBoolean(false);

        ContinuableFuture<String> future = executor.execute(() -> {
            throw new RuntimeException("Test exception");
        }, () -> finalExecuted.set(true));

        Assertions.assertThrows(Exception.class, () -> future.get());
        Assertions.assertTrue(finalExecuted.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableCollectionEmpty() {
        AsyncExecutor executor = new AsyncExecutor();
        List<Callable<Integer>> tasks = new ArrayList<>();
        List<ContinuableFuture<Integer>> futures = executor.execute(tasks);
        Assertions.assertTrue(futures.isEmpty());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableCollectionNull() {
        AsyncExecutor executor = new AsyncExecutor();
        List<ContinuableFuture<Integer>> futures = executor.execute((Collection<Callable<Integer>>) null);
        Assertions.assertTrue(futures.isEmpty());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableCollection() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        List<Callable<Integer>> tasks = Arrays.asList(() -> 1, () -> 2, () -> 3);
        List<ContinuableFuture<Integer>> futures = executor.execute(tasks);

        Assertions.assertEquals(3, futures.size());

        int sum = 0;
        for (ContinuableFuture<Integer> future : futures) {
            sum += future.get();
        }

        Assertions.assertEquals(6, sum);
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithRetryNoRetryNeeded() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            attempts.incrementAndGet();
        }, 3, 10, e -> e instanceof RuntimeException);

        future.get();
        Assertions.assertEquals(1, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithRetry() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Retry needed");
            }
        }, 2, 10, e -> e instanceof RuntimeException);

        future.get();
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithRetryMaxExceeded() {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Always fails");
        }, 2, 10, e -> e instanceof RuntimeException);

        Assertions.assertThrows(Exception.class, () -> future.get());
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteRunnableWithRetryConditionNotMet() {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<Void> future = executor.execute((Throwables.Runnable<Exception>) () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("Different exception");
        }, 2, 10, e -> e instanceof RuntimeException);

        Assertions.assertThrows(Exception.class, () -> future.get());
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithRetryNoRetryNeeded() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<String> future = executor.execute(() -> {
            attempts.incrementAndGet();
            return "Success";
        }, 3, 10, (result, exception) -> result == null);

        String result = future.get();
        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(1, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithRetryOnNullResult() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<String> future = executor.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                return null;
            }
            return "Success";
        }, 3, 10, (result, exception) -> result == null);

        String result = future.get();
        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(2, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithRetryOnException() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<String> future = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Retry needed");
            }
            return "Success";
        }, 3, 10, (result, exception) -> exception != null);

        String result = future.get();
        Assertions.assertEquals("Success", result);
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteCallableWithRetryMaxExceeded() {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        ContinuableFuture<String> future = executor.execute(() -> {
            attempts.incrementAndGet();
            return null;
        }, 2, 10, (result, exception) -> result == null);

        Assertions.assertThrows(Exception.class, () -> future.get());
        Assertions.assertEquals(3, attempts.get());
        executor.shutdown();
    }

    @Test
    public void testGetExecutor() {
        AsyncExecutor executor = new AsyncExecutor();
        Executor executor1 = executor.getExecutor();
        Executor executor2 = executor.getExecutor();
        Assertions.assertNotNull(executor1);
        Assertions.assertSame(executor1, executor2);
        executor.shutdown();
    }

    @Test
    public void testGetExecutorWithProvidedExecutor() {
        ExecutorService javaExecutor = Executors.newFixedThreadPool(2);
        AsyncExecutor executor = new AsyncExecutor(javaExecutor);
        Executor returned = executor.getExecutor();
        Assertions.assertSame(javaExecutor, returned);
        executor.shutdown();
    }

    @Test
    public void testShutdown() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    public void testShutdownUninitializedExecutor() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.shutdown();
    }

    @Test
    public void testShutdownWithTimeout() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            Thread.sleep(100);
            latch.countDown();
        });

        executor.shutdown(2, TimeUnit.SECONDS);
        Assertions.assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void testShutdownWithZeroTimeout() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.execute((Throwables.Runnable<Exception>) () -> {
            Thread.sleep(100);
        });
        executor.shutdown(0, TimeUnit.SECONDS);
    }

    @Test
    public void testShutdownOnNonExecutorService() {
        Executor plainExecutor = new Executor() {
            @Override
            public void execute(java.lang.Runnable command) {
                command.run();
            }
        };
        AsyncExecutor executor = new AsyncExecutor(plainExecutor);
        executor.shutdown(1, TimeUnit.SECONDS);
    }

    @Test
    public void testIsTerminatedBeforeShutdown() {
        AsyncExecutor executor = new AsyncExecutor();
        executor.execute((Throwables.Runnable<Exception>) () -> {
            Thread.sleep(50);
        });
        Assertions.assertFalse(executor.isTerminated());
        executor.shutdown();
    }

    @Test
    public void testIsTerminatedAfterShutdown() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute((Throwables.Runnable<Exception>) () -> {
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);
        executor.shutdown(2, TimeUnit.SECONDS);
        Assertions.assertTrue(executor.isTerminated());
    }

    @Test
    public void testIsTerminatedUninitialized() {
        AsyncExecutor executor = new AsyncExecutor();
        Assertions.assertTrue(executor.isTerminated());
    }

    @Test
    public void testIsTerminatedOnNonExecutorService() {
        Executor plainExecutor = new Executor() {
            @Override
            public void execute(java.lang.Runnable command) {
                command.run();
            }
        };
        AsyncExecutor executor = new AsyncExecutor(plainExecutor);
        Assertions.assertTrue(executor.isTerminated());
    }

    @Test
    public void testToStringDefault() {
        AsyncExecutor executor = new AsyncExecutor();
        String str = executor.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("coreThreadPoolSize"));
        Assertions.assertTrue(str.contains("maxThreadPoolSize"));
        Assertions.assertTrue(str.contains("keepAliveTime"));
        executor.shutdown();
    }

    @Test
    public void testToStringCustom() {
        AsyncExecutor executor = new AsyncExecutor(5, 10, 90L, TimeUnit.SECONDS);
        String str = executor.toString();
        Assertions.assertTrue(str.contains("coreThreadPoolSize: 5"));
        Assertions.assertTrue(str.contains("maxThreadPoolSize: 10"));
        Assertions.assertTrue(str.contains("keepAliveTime: 90000ms"));
        executor.shutdown();
    }

    @Test
    public void testToStringBeforeInitialization() {
        AsyncExecutor executor = new AsyncExecutor(3, 6, 45L, TimeUnit.SECONDS);
        String str = executor.toString();
        Assertions.assertTrue(str.contains("coreThreadPoolSize: 3"));
        executor.shutdown();
    }

    @Test
    public void testToStringAfterInitialization() {
        AsyncExecutor executor = new AsyncExecutor(3, 6, 45L, TimeUnit.SECONDS);
        executor.getExecutor();
        String str = executor.toString();
        Assertions.assertTrue(str.contains("coreThreadPoolSize: 3"));
        Assertions.assertTrue(str.contains("activeCount"));
        executor.shutdown();
    }

    @Test
    public void testConcurrentExecution() throws Exception {
        AsyncExecutor executor = new AsyncExecutor(4, 8, 60L, TimeUnit.SECONDS);
        AtomicInteger counter = new AtomicInteger(0);
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
        Assertions.assertEquals(taskCount, counter.get());

        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
    }

    @Test
    public void testGetExecutorThreadSafety() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();
        AtomicReference<Executor> executor1 = new AtomicReference<>();
        AtomicReference<Executor> executor2 = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            executor1.set(executor.getExecutor());
            latch.countDown();
        });

        Thread t2 = new Thread(() -> {
            executor2.set(executor.getExecutor());
            latch.countDown();
        });

        t1.start();
        t2.start();
        latch.await(2, TimeUnit.SECONDS);

        Assertions.assertSame(executor1.get(), executor2.get());
        executor.shutdown();
    }

    @Test
    public void testExecuteComplexTask() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<Integer> future = executor.execute(() -> {
            int sum = 0;
            for (int i = 1; i <= 100; i++) {
                sum += i;
            }
            return sum;
        });

        Integer result = future.get();
        Assertions.assertEquals(5050, result);
        executor.shutdown();
    }

    @Test
    public void testExecuteWithChaining() throws Exception {
        AsyncExecutor executor = new AsyncExecutor();

        ContinuableFuture<Integer> future = executor.execute(() -> 10).thenCallAsync(x -> x * 2).thenCallAsync(x -> x + 5);

        Integer result = future.get();
        Assertions.assertEquals(25, result);
        executor.shutdown();
    }
}
