/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * The AsyncExecutor class provides a convenient way to execute tasks asynchronously
 * using a configurable thread pool. It supports both Runnable and Callable tasks,
 * with additional features like retry mechanisms and batch execution.
 * 
 * <p>By default, the core pool size is the maximum of 8 and the number of available processors,
 * and the maximum pool size is the maximum of 16 and twice the number of available processors.
 * The default keep-alive time is 180 seconds.</p>
 * 
 * <p>The executor automatically registers a shutdown hook to ensure proper cleanup
 * when the JVM exits.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * AsyncExecutor executor = new AsyncExecutor();
 * 
 * // Execute a simple task
 * ContinuableFuture<Void> future = executor.execute(() -> {
 *     System.out.println("Task executed asynchronously");
 * });
 * 
 * // Execute a task that returns a result
 * ContinuableFuture<String> resultFuture = executor.execute(() -> {
 *     return "Task completed";
 * });
 * 
 * // Execute with retry
 * ContinuableFuture<String> retryFuture = executor.execute(
 *     () -> performNetworkCall(),
 *     3, // retry 3 times
 *     1000, // wait 1 second between retries
 *     (result, exception) -> exception != null // retry on any exception
 * );
 * }</pre>
 *
 * @see ContinuableFuture
 * @see Futures
 * @see Fn
 * @see Fnn
 * @since 1.0
 */
public class AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(8, InternalUtil.CPU_CORES);

    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Math.max(16, InternalUtil.CPU_CORES * 2);

    private final int coreThreadPoolSize;

    private final int maxThreadPoolSize;

    private final long keepAliveTime;

    private final TimeUnit unit;

    private volatile Executor executor; //NOSONAR

    /**
     * Constructs an AsyncExecutor with default configuration.
     * 
     * <p>Default values:</p>
     * <ul>
     *   <li>Core pool size: max(8, number of CPU cores)</li>
     *   <li>Maximum pool size: max(16, 2 * number of CPU cores)</li>
     *   <li>Keep-alive time: 180 seconds</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * executor.execute(() -> System.out.println("Hello from async task"));
     * }</pre>
     */
    public AsyncExecutor() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 180L, TimeUnit.SECONDS);
    }

    /**
     * Constructs an AsyncExecutor with specified thread pool configuration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Create executor with 10 core threads, 20 max threads, 60 second keep-alive
     * AsyncExecutor executor = new AsyncExecutor(10, 20, 60L, TimeUnit.SECONDS);
     * }</pre>
     *
     * @param coreThreadPoolSize the number of threads to keep in the pool, even if they are idle
     * @param maxThreadPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @throws IllegalArgumentException if any of the arguments are negative, or if the maximum pool size is less than the core pool size
     */
    public AsyncExecutor(final int coreThreadPoolSize, final int maxThreadPoolSize, final long keepAliveTime, final TimeUnit unit)
            throws IllegalArgumentException {
        N.checkArgNotNegative(coreThreadPoolSize, cs.coreThreadPoolSize);
        N.checkArgNotNegative(maxThreadPoolSize, cs.maxThreadPoolSize);
        N.checkArgNotNegative(keepAliveTime, cs.keepAliveTime);
        N.checkArgNotNull(unit, cs.unit);

        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = Math.max(coreThreadPoolSize, maxThreadPoolSize);
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
    }

    /**
     * Constructs an AsyncExecutor that wraps an existing Executor.
     * 
     * <p>If the provided executor is a ThreadPoolExecutor, its configuration
     * parameters are extracted and used. Otherwise, default values are used.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ExecutorService customExecutor = Executors.newFixedThreadPool(5);
     * AsyncExecutor asyncExecutor = new AsyncExecutor(customExecutor);
     * }</pre>
     *
     * @param executor the Executor to be used for executing tasks
     */
    public AsyncExecutor(final Executor executor) {
        this(getCorePoolSize(executor), getMaximumPoolSize(executor), getKeepAliveTime(executor), TimeUnit.MILLISECONDS);

        this.executor = executor;
    }

    private static int getCorePoolSize(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getCorePoolSize() : DEFAULT_CORE_POOL_SIZE;
    }

    private static int getMaximumPoolSize(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getMaximumPoolSize() : DEFAULT_MAX_THREAD_POOL_SIZE;
    }

    private static long getKeepAliveTime(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getKeepAliveTime(TimeUnit.MILLISECONDS)
                : TimeUnit.SECONDS.toMillis(180);
    }

    /**
     * Executes the provided command asynchronously.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.execute(() -> {
     *     // Perform some asynchronous work
     *     processData();
     * });
     * 
     * future.thenRun(() -> System.out.println("Task completed"));
     * }</pre>
     *
     * @param command the Runnable to be executed asynchronously
     * @return a ContinuableFuture representing the result of the computation
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command) {
        return execute(new FutureTask<>(() -> {
            command.run();
            return null;
        }));
    }

    /**
     * Executes the provided command asynchronously and performs a final action after the command execution.
     * 
     * <p>The final action is guaranteed to execute regardless of whether the command
     * completes successfully or throws an exception.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.execute(
     *     () -> processData(),
     *     () -> releaseResources() // Always executed
     * );
     * }</pre>
     *
     * @param command the Runnable to be executed asynchronously
     * @param actionInFinal the Runnable to be executed after the command (in a finally block)
     * @return a ContinuableFuture representing the result of the computation
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command, final java.lang.Runnable actionInFinal) {
        return execute(new FutureTask<>(() -> {
            try {
                command.run();
                return null;
            } finally {
                actionInFinal.run();
            }
        }));
    }

    //    /**
    //     *
    //     * @param commands
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SafeVarargs
    //    public final List<ContinuableFuture<Void>> execute(final Throwables.Runnable<? extends Exception>... commands) {
    //        if (N.isEmpty(commands)) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.length);
    //
    //        for (Throwables.Runnable<? extends Exception> command : commands) {
    //            results.add(execute(command));
    //        }
    //
    //        return results;
    //    }

    /**
     * Executes a list of commands asynchronously.
     * 
     * <p>Each command is executed independently and returns its own ContinuableFuture.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<Throwables.Runnable<Exception>> tasks = Arrays.asList(
     *     () -> processFile1(),
     *     () -> processFile2(),
     *     () -> processFile3()
     * );
     * 
     * List<ContinuableFuture<Void>> futures = executor.execute(tasks);
     * 
     * // Wait for all tasks to complete
     * Futures.allOf(futures).get();
     * }</pre>
     *
     * @param commands the list of Runnable commands to be executed asynchronously
     * @return a list of ContinuableFutures representing the results of the computations
     */
    public List<ContinuableFuture<Void>> execute(final List<? extends Throwables.Runnable<? extends Exception>> commands) {
        if (N.isEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.size());

        for (final Throwables.Runnable<? extends Exception> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    /**
     * Executes the provided Callable command asynchronously.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<String> future = executor.execute(() -> {
     *     // Perform computation
     *     return "Result: " + calculateValue();
     * });
     * 
     * String result = future.get();
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable to be executed asynchronously
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command) {
        return execute(new FutureTask<>(command));
    }

    /**
     * Executes the provided Callable command asynchronously and performs a final action.
     * 
     * <p>The final action is guaranteed to execute regardless of whether the command
     * completes successfully or throws an exception.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = executor.execute(
     *     () -> computeValue(),
     *     () -> logCompletion() // Always executed
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable to be executed asynchronously
     * @param actionInFinal the Runnable to be executed after the command (in a finally block)
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command, final java.lang.Runnable actionInFinal) {
        return execute(new FutureTask<>(() -> {
            try {
                return command.call();
            } finally {
                actionInFinal.run();
            }
        }));
    }

    //    /**
    //     *
    //     * @param <R>
    //     * @param commands
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SafeVarargs
    //    public final <R> List<ContinuableFuture<R>> execute(final Callable<R>... commands) {
    //        if (N.isEmpty(commands)) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.length);
    //
    //        for (Callable<R> command : commands) {
    //            results.add(execute(command));
    //        }
    //
    //        return results;
    //    }

    /**
     * Executes a collection of Callable commands asynchronously.
     * 
     * <p>Each command is executed independently and returns its own ContinuableFuture.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * List<Callable<Integer>> tasks = Arrays.asList(
     *     () -> computeValue1(),
     *     () -> computeValue2(),
     *     () -> computeValue3()
     * );
     * 
     * List<ContinuableFuture<Integer>> futures = executor.execute(tasks);
     * 
     * // Get all results
     * List<Integer> results = futures.stream()
     *     .map(ContinuableFuture::get)
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callables
     * @param commands the collection of Callable commands to be executed asynchronously
     * @return a list of ContinuableFutures representing the results of the computations
     */
    public <R> List<ContinuableFuture<R>> execute(final Collection<? extends Callable<R>> commands) {
        if (N.isEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.size());

        for (final Callable<R> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    /**
     * Executes a Runnable command asynchronously with retry mechanism.
     * 
     * <p>The command will be retried if it fails and the retry condition is met.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.execute(
     *     () -> sendEmail(),
     *     3, // retry up to 3 times
     *     1000, // wait 1 second between retries
     *     e -> e instanceof IOException // retry only on IOException
     * );
     * }</pre>
     *
     * @param action the Runnable to be executed asynchronously
     * @param retryTimes the maximum number of retry attempts
     * @param retryIntervalInMillis the interval in milliseconds between retry attempts
     * @param retryCondition the condition to determine whether to retry based on the exception
     * @return a ContinuableFuture representing the result of the computation
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> action, final int retryTimes, final long retryIntervalInMillis,
            final Predicate<? super Exception> retryCondition) {
        return execute(() -> {
            Retry.of(retryTimes, retryIntervalInMillis, retryCondition).run(action);
            return null;
        });
    }

    /**
     * Executes a Callable command asynchronously with retry mechanism.
     * 
     * <p>The command will be retried if it fails or returns an unsatisfactory result
     * according to the retry condition.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ContinuableFuture<String> future = executor.execute(
     *     () -> fetchDataFromAPI(),
     *     5, // retry up to 5 times
     *     2000, // wait 2 seconds between retries
     *     (result, exception) -> exception != null || result == null // retry on exception or null result
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callable
     * @param action the Callable to be executed asynchronously
     * @param retryTimes the maximum number of retry attempts
     * @param retryIntervalInMillis the interval in milliseconds between retry attempts
     * @param retryCondition the condition to determine whether to retry based on the result and exception
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> action, final int retryTimes, final long retryIntervalInMillis,
            final BiPredicate<? super R, ? super Exception> retryCondition) {
        return execute(() -> {
            final Retry<R> retry = Retry.of(retryTimes, retryIntervalInMillis, retryCondition);
            return retry.call(action);
        });
    }

    /**
     * Executes a FutureTask asynchronously.
     * This is a protected method used internally by other execute methods.
     *
     * @param <R> the type of the result
     * @param futureTask the FutureTask to execute
     * @return a ContinuableFuture wrapping the FutureTask
     */
    protected <R> ContinuableFuture<R> execute(final FutureTask<R> futureTask) {
        final Executor executor = getExecutor(); //NOSONAR

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Retrieves the executor used by this AsyncExecutor.
     * If the executor is not initialized, it creates a new ThreadPoolExecutor
     * with the configured parameters and registers a shutdown hook.
     * 
     * <p>This method is marked as @Internal and is primarily for framework use.</p>
     *
     * @return the Executor used by this AsyncExecutor
     */
    @Internal
    public Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    @SuppressWarnings("UnnecessaryLocalVariable")
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit,
                            new LinkedBlockingQueue<>());
                    //    if (keepAliveTime > 0 && coreThreadPoolSize == maxThreadPoolSize) {
                    //        threadPoolExecutor.allowCoreThreadTimeOut(true);
                    //    }

                    executor = threadPoolExecutor;

                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            shutdown(120, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            logger.warn("Error during shutdown: " + e.getMessage(), e);
                        }
                    }));
                }
            }
        }

        return executor;
    }

    /**
     * Shuts down the executor used by this AsyncExecutor.
     * 
     * <p>This method initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * // Use executor...
     * executor.shutdown(); // Gracefully shutdown
     * }</pre>
     */
    public synchronized void shutdown() {
        shutdown(0, TimeUnit.SECONDS);
    }

    /**
     * Shuts down the executor with a specified timeout for termination.
     * 
     * <p>This method initiates an orderly shutdown and waits for the specified
     * duration for tasks to complete. If tasks are still running after the
     * timeout, the method returns without forcing termination.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * // Use executor...
     * executor.shutdown(30, TimeUnit.SECONDS); // Wait up to 30 seconds for tasks to complete
     * }</pre>
     *
     * @param terminationTimeout the maximum time to wait for executor termination
     * @param timeUnit the time unit of the terminationTimeout argument
     */
    public synchronized void shutdown(final long terminationTimeout, final TimeUnit timeUnit) {
        if (executor == null || !(executor instanceof ExecutorService executorService)) {
            return;
        }

        logger.warn("Starting to shutdown task in AsyncExecutor");

        try {
            executorService.shutdown();

            if (terminationTimeout > 0 && !executorService.isTerminated()) {
                //noinspection ResultOfMethodCallIgnored
                executorService.awaitTermination(terminationTimeout, timeUnit);
            }
        } catch (final InterruptedException e) {
            logger.warn("Not all the requests/tasks executed in AsyncExecutor are completed successfully before shutdown.");
        } finally {
            executor = null;
            logger.warn("Completed to shutdown task in AsyncExecutor");
        }
    }

    /**
     * Checks if the executor has been terminated.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * if (executor.isTerminated()) {
     *     System.out.println("Executor has been shut down");
     * }
     * }</pre>
     *
     * @return {@code true} if the executor has been terminated, {@code false} otherwise
     */
    public boolean isTerminated() {
        return executor == null || !(executor instanceof ExecutorService executorService) || executorService.isTerminated();
    }

    /**
     * Returns a string representation of this AsyncExecutor.
     * 
     * <p>The string includes configuration parameters and current state information
     * such as core pool size, maximum pool size, active count (if available),
     * keep-alive time, and executor details.</p>
     *
     * @return a string representation of this AsyncExecutor
     */
    @Override
    public String toString() {
        final String activeCount = executor instanceof ThreadPoolExecutor ? "" + ((ThreadPoolExecutor) executor).getActiveCount() : "?";

        return "{coreThreadPoolSize: " + coreThreadPoolSize + ", maxThreadPoolSize: " + maxThreadPoolSize + ", activeCount: " + activeCount
                + ", keepAliveTime: " + unit.toMillis(keepAliveTime) + "ms, Executor: " + N.toString(executor) + "}";
    }
}