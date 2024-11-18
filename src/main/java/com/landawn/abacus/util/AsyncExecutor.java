/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * The AsyncExecutor class is used to execute tasks asynchronously.
 * It provides methods to execute Runnable and Callable tasks, with options for retrying tasks if they fail.
 * <br />
 * By default, the core pool size is the maximum of 8 and the number of available processors,
 * the maximum pool size is the maximum of 16 and twice the number of available processors,
 *
 * @see ContinuableFuture
 * @see Futures
 * @see Fn
 * @see Fn.Fnn
 *
 */
public class AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(8, IOUtil.CPU_CORES);

    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Math.max(16, IOUtil.CPU_CORES * 2);

    private final int coreThreadPoolSize;

    private final int maxThreadPoolSize;

    private final long keepAliveTime;

    private final TimeUnit unit;

    private volatile Executor executor; //NOSONAR

    /**
     * Constructs an instance of AsyncExecutor with default core pool size, maximum pool size, keep alive time, and time unit
     * It initializes the AsyncExecutor with the default core pool size, maximum pool size, keep alive time, and time unit.
     * The default core pool size is the maximum of 8 and the number of available processors.
     * The default maximum pool size is the maximum of 16 and twice the number of available processors.
     * The default keep alive time is 180 seconds.
     */
    public AsyncExecutor() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 180L, TimeUnit.SECONDS);
    }

    /**
     * Constructs an instance of AsyncExecutor with the specified core pool size, maximum pool size, keep alive time, and time unit.
     * The core pool size is the number of threads to keep in the pool, even if they are idle.
     * The maximum pool size is the maximum number of threads to allow in the pool.
     * The keep alive time is when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
     * The time unit is the time unit for the keepAliveTime argument.
     *
     * @param coreThreadPoolSize the number of threads to keep in the pool, even if they are idle
     * @param maxThreadPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @throws IllegalArgumentException if any of the arguments are negative or if the maximum pool size is less than the core pool size
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
     * Constructs an instance of AsyncExecutor with the specified Executor.
     * The Executor is used to execute tasks asynchronously.
     * The core pool size, maximum pool size, keep alive time, and time unit of the AsyncExecutor are derived from the provided Executor.
     * If the Executor is not an instance of ThreadPoolExecutor, default values are used.
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
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getMaximumPoolSize() : DEFAULT_CORE_POOL_SIZE;
    }

    private static long getKeepAliveTime(final Executor executor) {
        return executor instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor) executor).getKeepAliveTime(TimeUnit.MILLISECONDS)
                : TimeUnit.SECONDS.toMillis(180);
    }

    /**
     * Executes the provided command asynchronously.
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
     * @param command the Runnable to be executed asynchronously
     * @param actionInFinal the Runnable to be executed after the command
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
     * Executes a list of provided commands asynchronously.
     *
     * @param commands the list of Runnable to be executed asynchronously
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
     * Executes the provided command asynchronously.
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable to be executed asynchronously
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command) {
        return execute(new FutureTask<>(command));
    }

    /**
     * Executes the provided command asynchronously and performs a final action after the command execution.
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable to be executed asynchronously
     * @param actionInFinal the Runnable to be executed after the command
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> command, final java.lang.Runnable actioInFinal) {
        return execute(new FutureTask<>(() -> {
            try {
                return command.call();
            } finally {
                actioInFinal.run();
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
     * Executes a collection of provided commands asynchronously.
     *
     * @param <R> the type of the result returned by the Callable
     * @param commands the collection of Callable to be executed asynchronously
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
     * Executes the provided command asynchronously with retry mechanism.
     * The method retries the execution of the command for a specified number of times if it fails, with a specified interval between retries.
     * The retryCondition is a Predicate that determines whether to retry the execution based on the exception thrown.
     *
     * @param action the Runnable to be executed asynchronously
     * @param retryTimes the number of times to retry the execution if it fails
     * @param retryIntervallInMillis the interval in milliseconds between retries
     * @param retryCondition the condition to determine whether to retry the execution based on the exception thrown
     * @return a ContinuableFuture representing the result of the computation
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> action, final int retryTimes, final long retryIntervallInMillis,
            final Predicate<? super Exception> retryCondition) {
        return execute((Callable<Void>) () -> {
            Retry.of(retryTimes, retryIntervallInMillis, retryCondition).run(action);
            return null;
        });
    }

    /**
     * Executes the provided command asynchronously with a retry mechanism.
     * The method retries the execution of the command for a specified number of times if it fails, with a specified interval between retries.
     * The retryCondition is a BiPredicate that determines whether to retry the execution based on the result and the exception thrown.
     *
     * @param <R> the type of the result returned by the Callable
     * @param action the Callable to be executed asynchronously
     * @param retryTimes the number of times to retry the execution if it fails
     * @param retryIntervallInMillis the interval in milliseconds between retries
     * @param retryCondition the condition to determine whether to retry the execution based on the result and the exception thrown
     * @return a ContinuableFuture representing the result of the computation
     */
    public <R> ContinuableFuture<R> execute(final Callable<R> action, final int retryTimes, final long retryIntervallInMillis,
            final BiPredicate<? super R, ? super Exception> retryCondition) {
        return execute((Callable<R>) () -> {
            final Retry<R> retry = Retry.of(retryTimes, retryIntervallInMillis, retryCondition);
            return retry.call(action);
        });
    }

    /**
     *
     * @param <R>
     * @param futureTask
     * @return
     */
    protected <R> ContinuableFuture<R> execute(final FutureTask<R> futureTask) {
        final Executor executor = getExecutor(); //NOSONAR

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Retrieves the executor used by this AsyncExecutor.
     * If the executor is not initialized, it initializes a new ThreadPoolExecutor with the specified core pool size, maximum pool size, keep alive time, and time unit.
     * It also adds a shutdown hook to the JVM to ensure the executor is properly shutdown when the JVM exits.
     *
     * @return the Executor used by this AsyncExecutor
     */
    @Internal
    public Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit,
                            new LinkedBlockingQueue<>());
                    //    if (keepAliveTime > 0 && coreThreadPoolSize == maxThreadPoolSize) {
                    //        threadPoolExecutor.allowCoreThreadTimeOut(true);
                    //    }

                    executor = threadPoolExecutor;

                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            shutdown(120, TimeUnit.SECONDS);
                        }
                    });
                }
            }
        }

        return executor;
    }

    /**
     * Shuts down the executor used by this AsyncExecutor.
     * This method initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * This method does not wait for previously submitted tasks to complete execution.
     */
    public synchronized void shutdown() {
        shutdown(0, TimeUnit.SECONDS);
    }

    /**
     * Shuts down the executor used by this AsyncExecutor with a specified timeout.
     * This method initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted.
     * If the executor is not terminated when the timeout is reached, the method will stop waiting and return.
     * Invocation has no additional effect if already shut down.
     * This method does not wait for previously submitted tasks to complete execution beyond the specified timeout.
     *
     * @param terminationTimeout the maximum time to wait for the executor to terminate
     * @param timeUnit the time unit of the terminationTimeout argument
     */
    public synchronized void shutdown(final long terminationTimeout, final TimeUnit timeUnit) {
        if (executor == null || !(executor instanceof final ExecutorService executorService)) {
            return;
        }

        logger.warn("Starting to shutdown task in AsyncExecutor");

        try {
            executorService.shutdown();

            if (terminationTimeout > 0 && !executorService.isTerminated()) {
                executorService.awaitTermination(terminationTimeout, timeUnit);
            }
        } catch (final InterruptedException e) {
            logger.warn("Not all the requests/tasks executed in AsyncExecutor are completed successfully before shutdown.");
        } finally {
            executor = null;
            logger.warn("Completed to shutdown task in AsyncExecutor");
        }
    }

    @Override
    public String toString() {
        final String activeCount = executor instanceof ThreadPoolExecutor ? "" + ((ThreadPoolExecutor) executor).getActiveCount() : "?";

        return "{coreThreadPoolSize: " + coreThreadPoolSize + ", maxThreadPoolSize: " + maxThreadPoolSize + ", activeCount: " + activeCount
                + ", keepAliveTime: " + unit.toMillis(keepAliveTime) + "ms, Executor: " + N.toString(executor) + "}";
    }
}
