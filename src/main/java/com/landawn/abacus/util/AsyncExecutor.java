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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * Provides a convenient way to execute tasks asynchronously using a configurable
 * thread pool. It supports both {@link java.lang.Runnable} and {@link java.util.concurrent.Callable}
 * tasks, with additional features such as retry mechanisms and batch execution. Each
 * submitted task yields a {@link ContinuableFuture} for composing follow-up actions.
 *
 * <p>By default, the core pool size is the maximum of 8 and the number of available processors,
 * and the maximum pool size is the maximum of 16 and twice the number of available processors.
 * The default keep-alive time is 180 seconds.</p>
 *
 * <p>When the internal thread pool is lazily created, a shutdown hook is registered on a best-effort basis
 * to ensure proper cleanup when the JVM exits; if the JVM is already shutting down no hook can be registered and
 * the pool is used without one (its worker threads are daemon threads, so JVM exit is never blocked).
 * No hook is registered for an externally supplied {@code Executor}.</p>
 *
 * <p>The worker threads of that internal pool are <b>daemon</b> threads named
 * {@code abacus-async-<poolIndex>-<threadIndex>}, so an application that never calls {@link #shutdown()} can
 * still exit normally. In-flight and queued work is not abandoned: the shutdown hook runs
 * {@link #shutdownAndAwait(long, TimeUnit)} and waits up to 120 seconds (by default) for the pool to drain.
 * That wait can be changed with the {@code abacus.asyncExecutor.shutdownHookTimeoutMillis} system property,
 * which is read once when this class is initialized; {@code 0} makes the hook return without waiting. An
 * externally supplied {@code Executor} keeps whatever threads its own factory creates.</p>
 *
 * <p><b>Executor ownership:</b> an instance created by one of the sizing constructors owns the pool it
 * creates lazily and shuts it down on {@link #shutdown()}. An instance created by
 * {@link #AsyncExecutor(Executor)} only <i>borrows</i> the supplied executor: {@code shutdown()} stops
 * this instance from accepting new work and waits for the tasks it submitted, but never shuts the
 * borrowed executor down - it may be shared with the rest of the application.</p>
 *
 * <p><b>Usage Examples:</b></p>
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
 * ContinuableFuture<String> retryFuture = executor.executeWithRetry(
 *     () -> performNetworkCall(),
 *     3,
 *     1000,
 *     (result, exception) -> exception != null // retry on any exception
 * );
 * }</pre>
 *
 * @see ContinuableFuture
 * @see Futures
 * @see Fn
 * @see Fnn
 * @see MoreExecutors
 */
public class AsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(8, InternalUtil.CPU_CORES);

    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Math.max(16, InternalUtil.CPU_CORES * 2);

    /** Distinguishes the worker threads of concurrently live {@code AsyncExecutor} instances by name. */
    private static final AtomicInteger POOL_INDEX = new AtomicInteger();

    /**
     * How long the JVM shutdown hook waits for in-flight tasks to finish, in milliseconds. Configurable
     * through the {@code abacus.asyncExecutor.shutdownHookTimeoutMillis} system property (read once, at
     * class initialization); {@code 0} makes the hook return without waiting.
     */
    private static final long SHUTDOWN_HOOK_TIMEOUT_MILLIS = parseShutdownHookTimeoutMillis();

    private static long parseShutdownHookTimeoutMillis() {
        final String value = System.getProperty("abacus.asyncExecutor.shutdownHookTimeoutMillis");

        if (Strings.isEmpty(value)) {
            return 120_000L;
        }

        try {
            return Math.max(0, Long.parseLong(value.trim()));
        } catch (final NumberFormatException e) {
            logger.warn("Ignoring non-numeric value of system property 'abacus.asyncExecutor.shutdownHookTimeoutMillis': " + value);

            return 120_000L;
        }
    }

    private final int coreThreadPoolSize;

    private final int maxThreadPoolSize;

    private final long keepAliveTime;

    private final TimeUnit unit;

    private volatile Executor executor; //NOSONAR

    /**
     * {@code false} when this instance merely wraps a caller-supplied {@code Executor}. A borrowed
     * executor is never shut down and never gets a shutdown hook: its lifecycle belongs to whoever
     * created it, and shutting down a shared application-wide pool from a wrapper is not recoverable.
     */
    private final boolean ownsExecutor;

    /**
     * Tasks submitted through this instance that have not finished yet. For a borrowed executor the
     * delegate's own {@code isTerminated()} says nothing about this wrapper (it is shared, and this
     * wrapper never shuts it down), so termination has to be expressed in terms of the work this
     * wrapper actually submitted. Each entry is removed by the task that owns it;
     * {@link #reclaimAbandonedTasks()} removes the ones a delegate accepted and then never ran.
     */
    private final Set<SubmittedTask> activeTasks = ConcurrentHashMap.newKeySet();

    private volatile ExecutorService shutdownExecutorService;

    private volatile boolean isShutdown = false;

    // The JVM shutdown hook registered when the executor is lazily created; kept so shutdown() can
    // remove it — otherwise every created-then-discarded AsyncExecutor leaks a hook (and its executor).
    private volatile Thread shutdownHook;

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * executor.execute(() -> System.out.println("Hello from async task"));
     * }</pre>
     *
     */
    public AsyncExecutor() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 180L, TimeUnit.SECONDS);
    }

    /**
     * Constructs an AsyncExecutor with specified thread pool configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create executor with 10 core threads, 20 max threads, 60 second keep-alive
     * AsyncExecutor executor = new AsyncExecutor(10, 20, 60L, TimeUnit.SECONDS);
     * }</pre>
     *
     * @param coreThreadPoolSize the number of threads to keep in the pool, even if they are idle
     * @param maxThreadPoolSize the maximum number of threads to allow in the pool; if less than {@code coreThreadPoolSize}, it is raised to {@code coreThreadPoolSize}.
     *        Note that this bound is effectively unreachable with the unbounded queue this class uses - see
     *        {@link #getExecutor()} - so the pool runs at {@code coreThreadPoolSize} and queues the remainder
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @throws IllegalArgumentException if {@code coreThreadPoolSize} is negative, if {@code maxThreadPoolSize} is
     *         negative, if {@code coreThreadPoolSize} and {@code maxThreadPoolSize} are both zero, if
     *         {@code keepAliveTime} is negative, or if {@code unit} is {@code null}.
     */
    public AsyncExecutor(final int coreThreadPoolSize, final int maxThreadPoolSize, final long keepAliveTime, final TimeUnit unit)
            throws IllegalArgumentException {
        this(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit, true);
    }

    /**
     * @throws IllegalArgumentException if either pool size or {@code keepAliveTime} is negative, both pool sizes are zero, or {@code unit} is {@code null}
     */
    private AsyncExecutor(final int coreThreadPoolSize, final int maxThreadPoolSize, final long keepAliveTime, final TimeUnit unit, final boolean ownsExecutor)
            throws IllegalArgumentException {
        N.checkArgNotNegative(coreThreadPoolSize, cs.coreThreadPoolSize);
        N.checkArgNotNegative(maxThreadPoolSize, cs.maxThreadPoolSize);

        if (coreThreadPoolSize == 0 && maxThreadPoolSize == 0) {
            throw new IllegalArgumentException("coreThreadPoolSize and maxThreadPoolSize cannot both be zero");
        }
        N.checkArgNotNegative(keepAliveTime, cs.keepAliveTime);
        N.checkArgNotNull(unit, cs.unit);

        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = Math.max(coreThreadPoolSize, maxThreadPoolSize);
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.ownsExecutor = ownsExecutor;
    }

    /**
     * Constructs an AsyncExecutor that wraps an existing Executor.
     *
     * <p>If the provided executor is a ThreadPoolExecutor, its configuration
     * parameters are extracted and used. Otherwise, default values are used.</p>
     *
     * <p><b>This instance does not own {@code executor}.</b> {@link #shutdown()} and
     * {@link #shutdownAndAwait(long, TimeUnit)} stop this wrapper from accepting new work and wait for
     * the tasks <i>this wrapper</i> submitted, but they never call {@code shutdown()} on the supplied
     * executor - it may be shared with the rest of the application. No JVM shutdown hook is registered
     * for it either, and it keeps whatever threads its own factory creates. Shutting it down is the
     * caller's responsibility.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService customExecutor = Executors.newFixedThreadPool(5);
     * try {
     *     AsyncExecutor asyncExecutor = new AsyncExecutor(customExecutor);
     *     asyncExecutor.execute(() -> processData()).get();
     * } finally {
     *     // AsyncExecutor does not own an externally supplied executor.
     *     customExecutor.shutdown();
     * }
     * }</pre>
     *
     * @param executor the Executor to be used for executing tasks
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     * @see #shutdown()
     */
    public AsyncExecutor(final Executor executor) throws IllegalArgumentException {
        this(getCorePoolSize(checkExecutor(executor)), getMaximumPoolSize(executor), getKeepAliveTime(executor), TimeUnit.MILLISECONDS, false);

        this.executor = executor;
    }

    /**
     * @throws IllegalArgumentException if {@code executor} is {@code null}
     */
    private static Executor checkExecutor(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return executor;
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
     * Executes the provided command asynchronously using the underlying executor.
     *
     * <p>This method wraps the command in a FutureTask and submits it to the executor
     * for asynchronous execution. The command may throw checked exceptions which will be
     * captured in the returned ContinuableFuture.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.execute(() -> {
     *     // Perform some asynchronous work
     *     processData();
     * });
     *
     * future.thenRunAsync(() -> System.out.println("Task completed"));
     * }</pre>
     *
     * @param command the Runnable command to be executed asynchronously; may throw checked exceptions
     * @return a ContinuableFuture representing the pending completion of this action
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);

        return execute(new FutureTask<>(() -> {
            command.run();
            return null;
        }));
    }

    /**
     * Executes the provided command asynchronously and ensures a final action is performed after execution.
     *
     * <p>The final action is guaranteed to execute regardless of whether the command
     * completes successfully or throws an exception, similar to a try-finally block.
     * This is useful for cleanup operations such as releasing resources or updating state.</p>
     * If both actions fail, the command's failure remains primary and the final-action failure is
     * attached to it as a suppressed exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.execute(
     *     () -> processData(),
     *     () -> releaseResources() // invokes cleanup after command
     * );
     * }</pre>
     *
     * @param command the Runnable command to be executed asynchronously; may throw checked exceptions
     * @param finallyAction the Runnable to be executed after the command completes (in a finally block)
     * @return a ContinuableFuture representing the pending completion of this action
     * @throws IllegalArgumentException if any of {@code command}, {@code finallyAction} is {@code null}.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command, final java.lang.Runnable finallyAction)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(finallyAction, cs.finallyAction);

        return execute(new FutureTask<>(() -> {
            Throwable primary = null;
            try {
                command.run();
                return null;
            } catch (final Exception | Error e) {
                primary = e;
                throw e;
            } finally {
                try {
                    finallyAction.run();
                } catch (final RuntimeException | Error cleanupEx) {
                    if (primary != null && primary != cleanupEx) {
                        primary.addSuppressed(cleanupEx);
                    } else if (primary == null) {
                        throw cleanupEx;
                    }
                }
            }
        }));
    }

    /**
     * Executes a list of commands asynchronously in parallel.
     *
     * <p>Each command is submitted to the executor independently and returns its own ContinuableFuture.
     * The commands execute concurrently based on thread availability in the executor's thread pool.</p>
     *
     * <p>If the provided list is {@code null} or empty, returns an empty list.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param commands the list of Runnable commands to be executed asynchronously; may be {@code null} or empty
     * @return a list of ContinuableFutures representing the pending completion of this action for each command;
     *         returns an empty list if commands is {@code null} or empty
     * @throws IllegalArgumentException if any element of {@code commands} is {@code null}; the commands preceding
     *         that element have already been submitted - each may still be running or may already have
     *         completed - and their futures are not returned
     * @throws IllegalStateException if {@code commands} is non-empty and this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses a task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the refused task was not accepted, and the futures of the commands already submitted are
     *         not returned
     */
    public List<ContinuableFuture<Void>> execute(final List<? extends Throwables.Runnable<? extends Exception>> commands)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
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
     * Executes the provided Callable command asynchronously and returns its result.
     *
     * <p>This method wraps the Callable in a FutureTask and submits it to the executor
     * for asynchronous execution. The result can be retrieved from the returned
     * ContinuableFuture when the computation completes.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param command the Callable command to be executed asynchronously; may throw exceptions
     * @return a ContinuableFuture representing the pending result of this computation
     * @throws IllegalArgumentException if {@code command} is {@code null}.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public <R> ContinuableFuture<R> execute(final Callable<? extends R> command)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);

        return execute(new FutureTask<>(command));
    }

    /**
     * Executes the provided Callable command asynchronously and ensures a final action is performed after execution.
     *
     * <p>The final action is guaranteed to execute regardless of whether the command
     * completes successfully or throws an exception, similar to a try-finally block.
     * This is useful for cleanup operations such as releasing resources or logging completion.</p>
     * If both actions fail, the command's failure remains primary and the final-action failure is
     * attached to it as a suppressed exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = executor.execute(
     *     () -> computeValue(),
     *     () -> logCompletion() // invokes completion logging
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable command to be executed asynchronously; may throw exceptions
     * @param finallyAction the Runnable to be executed after the command completes (in a finally block)
     * @return a ContinuableFuture representing the pending result of this computation
     * @throws IllegalArgumentException if any of {@code command}, {@code finallyAction} is {@code null}.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public <R> ContinuableFuture<R> execute(final Callable<? extends R> command, final java.lang.Runnable finallyAction)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(finallyAction, cs.finallyAction);

        return execute(new FutureTask<>(() -> {
            Throwable primary = null;
            try {
                return command.call();
            } catch (final Exception | Error e) {
                primary = e;
                throw e;
            } finally {
                try {
                    finallyAction.run();
                } catch (final RuntimeException | Error cleanupEx) {
                    if (primary != null && primary != cleanupEx) {
                        primary.addSuppressed(cleanupEx);
                    } else if (primary == null) {
                        throw cleanupEx;
                    }
                }
            }
        }));
    }

    /**
     * Executes a collection of Callable commands asynchronously in parallel.
     *
     * <p>Each command is submitted to the executor independently and returns its own ContinuableFuture.
     * The commands execute concurrently based on thread availability in the executor's thread pool.</p>
     *
     * <p>If the provided collection is {@code null} or empty, returns an empty list.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * List<Integer> results = Futures.allOf(futures).get();
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callables
     * @param commands the collection of Callable commands to be executed asynchronously; may be {@code null} or empty
     * @return a list of ContinuableFutures representing the pending result of this computation for each command;
     *         returns an empty list if commands is {@code null} or empty
     * @throws IllegalArgumentException if any element of {@code commands} is {@code null}; the commands preceding
     *         that element have already been submitted - each may still be running or may already have
     *         completed - and their futures are not returned
     * @throws IllegalStateException if {@code commands} is non-empty and this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses a task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the refused task was not accepted, and the futures of the commands already submitted are
     *         not returned
     */
    public <R> List<ContinuableFuture<R>> execute(final Collection<? extends Callable<? extends R>> commands)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        if (N.isEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.size());

        for (final Callable<? extends R> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    /**
     * Executes a Runnable command asynchronously with automatic retry on failure.
     *
     * <p>The command will be retried up to the specified number of times if it fails and the
     * retry condition evaluates to {@code true}. A delay is introduced between retry attempts.</p>
     *
     * <p>The total number of execution attempts is retryTimes + 1 (initial attempt plus retries).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> future = executor.executeWithRetry(
     *     () -> sendEmail(),
     *     3,
     *     1000,
     *     e -> e instanceof IOException
     * );
     * }</pre>
     *
     * @param command the Runnable to be executed asynchronously; may throw checked exceptions
     * @param retryTimes the maximum number of retry attempts (0 means no retry, only initial attempt)
     * @param retryIntervalInMillis the interval in milliseconds to wait between retry attempts
     * @param retryCondition the predicate to determine whether to retry based on the caught exception;
     *                       receives the exception and returns {@code true} to retry, {@code false} to fail immediately
     * @return a ContinuableFuture representing the pending completion of this action (including retries)
     * @throws IllegalArgumentException if any of {@code command}, {@code retryCondition} is {@code null},
     *         or if {@code retryTimes} or {@code retryIntervalInMillis} is negative. All argument
     *         validation happens on the calling thread, before the task is submitted.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public ContinuableFuture<Void> executeWithRetry(final Throwables.Runnable<? extends Exception> command, final int retryTimes,
            final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(retryCondition, cs.retryCondition);

        // Build the policy on the calling thread so an invalid retryTimes/retryIntervalInMillis is reported
        // synchronously rather than from future.get() - matching executeWithRetry(Callable, ...) below.
        final Retry<Void> retry = Retry.withFixedDelay(retryTimes, retryIntervalInMillis, retryCondition);

        return execute(() -> {
            retry.run(command);
            return null;
        });
    }

    /**
     * Executes a Callable command asynchronously with automatic retry on failure or unsatisfactory result.
     *
     * <p>The command will be retried up to the specified number of times if the retry condition
     * evaluates to {@code true}. The retry condition can check both the result value and any exception thrown.
     * A delay is introduced between retry attempts.</p>
     *
     * <p>The total number of execution attempts is retryTimes + 1 (initial attempt plus retries).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = executor.executeWithRetry(
     *     () -> fetchDataFromAPI(),
     *     5,
     *     2000,
     *     (result, exception) -> exception != null || result == null // retry on exception or null result
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the Callable
     * @param command the Callable to be executed asynchronously; may throw exceptions
     * @param retryTimes the maximum number of retry attempts (0 means no retry, only initial attempt)
     * @param retryIntervalInMillis the interval in milliseconds to wait between retry attempts
     * @param retryCondition bi-predicate that receives the result (may be {@code null} on failure) and the exception
     *                       (may be {@code null} on success) and returns {@code true} to retry; must not be {@code null}
     * @return a ContinuableFuture representing the pending result of this computation (including retries)
     * @throws IllegalArgumentException if any of {@code command}, {@code retryCondition} is {@code null},
     *         or if {@code retryTimes} or {@code retryIntervalInMillis} is negative. All argument
     *         validation happens on the calling thread, before the task is submitted.
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    public <R> ContinuableFuture<R> executeWithRetry(final Callable<? extends R> command, final int retryTimes, final long retryIntervalInMillis,
            final BiPredicate<? super R, ? super Exception> retryCondition) throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(command, cs.command);
        N.checkArgNotNull(retryCondition, cs.retryCondition);

        // Build the policy on the calling thread so an invalid retryTimes/retryIntervalInMillis is reported
        // synchronously rather than from future.get() - matching executeWithRetry(Throwables.Runnable, ...) above.
        final Retry<R> retry = Retry.withFixedDelay(retryTimes, retryIntervalInMillis, retryCondition);

        return execute(() -> retry.call(command));
    }

    /**
     * Executes a FutureTask asynchronously using the underlying executor.
     *
     * <p>This is a protected method used internally by other execute methods.
     * It submits the FutureTask to the executor and wraps it in a ContinuableFuture
     * for enhanced composability.</p>
     *
     * @param <R> the type of the result produced by the FutureTask
     * @param futureTask the FutureTask to be executed asynchronously
     * @return a ContinuableFuture representing the pending result of this computation
     * @throws IllegalArgumentException if {@code futureTask} is {@code null}
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     * @throws RejectedExecutionException if the underlying executor refuses the task - for
     *         example a bounded, externally supplied executor whose queue is full, or a shutdown that raced this
     *         submission; the task was not accepted and no future is returned
     */
    protected <R> ContinuableFuture<R> execute(final FutureTask<? extends R> futureTask)
            throws IllegalArgumentException, IllegalStateException, RejectedExecutionException {
        N.checkArgNotNull(futureTask, cs.futureTask);

        final Executor executor;
        final SubmittedTask task;
        // Selection and reservation are one admission step. Shutdown must see the reservation
        // before dispatch leaves this lock; user executor callbacks run outside the lock.
        synchronized (this) {
            if (isShutdown) {
                throw new IllegalStateException("AsyncExecutor is shut down");
            }
            executor = getExecutor();
            if (isShutdown) {
                throw new IllegalStateException("AsyncExecutor is shut down");
            }
            task = new SubmittedTask(futureTask, executor);
            activeTasks.add(task);
        }

        // The task is submitted wrapped so that this instance knows how much of its own work is still
        // in flight. That is what isTerminated()/shutdownAndAwait(..) report for a borrowed executor,
        // whose own termination state belongs to its owner and may never be reached at all.
        try {
            executor.execute(() -> {
                // The claim can only be lost to reclaimAbandonedTasks(), which takes a reservation over
                // exclusively when the delegate has terminated - and a terminated executor is not running this.
                if (task.claim()) {
                    try {
                        futureTask.run();
                    } finally {
                        activeTasks.remove(task);
                    }
                }
            });
        } catch (final RuntimeException | Error e) {
            // Inline execution may have already released the reservation before done() throws.
            if (task.claim()) {
                activeTasks.remove(task);
            }
            throw e;
        }

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Releases the reservations of tasks that the executor they were handed to can no longer run.
     *
     * <p>A delegate may accept a task and then never run it: a {@code ThreadPoolExecutor} configured with
     * {@code DiscardPolicy}/{@code DiscardOldestPolicy} drops it silently, and a <i>borrowed</i> pool that its
     * owner later shuts down with {@code shutdownNow()} discards whatever it had queued. The wrapper submitted
     * for such a task never runs, so its reservation would be held for ever and pin {@link #isTerminated()} to
     * {@code false}. Once the delegate {@code ExecutorService} reports termination it is proven that the task
     * will never start, so the reservation is taken over here and the task is cancelled - which also releases
     * the caller's {@link ContinuableFuture} instead of leaving it waiting for a result that cannot arrive.</p>
     *
     * <p>A reservation is only reclaimed by winning {@link SubmittedTask#claim()} against the executor thread,
     * so a task that has already started keeps its reservation until it returns and is never cancelled
     * underneath running user code. A delegate that discards tasks without ever terminating - or a plain
     * {@code Executor}, which has no termination state at all - cannot be detected: nothing observable
     * distinguishes a dropped task from one that has not started yet.</p>
     */
    private void reclaimAbandonedTasks() {
        if (activeTasks.isEmpty()) {
            return;
        }

        // Every reservation in the set was handed to the same delegate: execute() dispatches to whatever
        // getExecutor() returns, and that is one executor for the whole life of this wrapper - it is created (or
        // supplied) once and getExecutor() throws once shutdown() has run, so no second one can ever be admitted.
        // The delegate's state is therefore worth asking for once, and the walk below is skipped while it is still
        // running - which is every scan that has nothing to reclaim. Walking it regardless made isTerminated()
        // cost ~6 ms with 100k reservations outstanding, and awaitActiveTasks() repeats the scan every 10 ms.
        final Iterator<SubmittedTask> iter = activeTasks.iterator();

        if (!iter.hasNext() || !iter.next().isDelegateTerminated()) {
            return;
        }

        for (final SubmittedTask task : activeTasks) {
            if (task.isDelegateTerminated() && task.claim()) {
                activeTasks.remove(task);
                task.cancel();
            }
        }
    }

    /**
     * One task submitted through this instance, together with the reservation it holds in
     * {@code activeTasks} and the executor it was handed to. {@code claimed} is won exactly once - by the
     * executor thread that is about to run the task, by the submitting thread when dispatch was rejected, or
     * by {@link #reclaimAbandonedTasks()} - which is what keeps those release paths from colliding.
     * Identity equality is deliberate: two submissions are never the same reservation.
     */
    private static final class SubmittedTask {
        private final FutureTask<?> futureTask;

        private final Executor executor;

        private final AtomicBoolean claimed = new AtomicBoolean();

        SubmittedTask(final FutureTask<?> futureTask, final Executor executor) {
            this.futureTask = futureTask;
            this.executor = executor;
        }

        boolean claim() {
            return claimed.compareAndSet(false, true);
        }

        boolean isDelegateTerminated() {
            return executor instanceof ExecutorService es && es.isTerminated();
        }

        void cancel() {
            futureTask.cancel(false);
        }
    }

    /**
     * Retrieves the underlying executor used by this AsyncExecutor, initializing it if necessary.
     *
     * <p>If the executor has not yet been initialized, this method creates a new ThreadPoolExecutor
     * with the configured parameters (core pool size, max pool size, keep-alive time) and an
     * unbounded LinkedBlockingQueue. A shutdown hook is registered on a best-effort basis to ensure
     * graceful termination when the JVM exits; if the JVM is already shutting down no hook can be registered
     * and the pool is used without one, with a warning logged.</p>
     *
     * <p><b>The configured maximum pool size is effectively unreachable.</b> A
     * {@link java.util.concurrent.ThreadPoolExecutor} only creates threads beyond its core size once its queue is
     * full, and the queue here is unbounded, so it never fills. The pool therefore grows to the core size and queues
     * everything after that; the maximum only takes effect if the queue is ever replaced with a bounded one. Size the
     * core pool for the concurrency you actually want, or supply your own {@link Executor}.</p>
     *
     * <p>The lazy initialization happens under this instance's lifecycle lock, so the pool is created at
     * most once no matter how many threads call this method.</p>
     *
     * <p>Work submitted straight to the returned {@code Executor} bypasses this instance's task
     * accounting. For an internally owned pool, {@link #shutdownAndAwait(long, TimeUnit)} and
     * {@link #isTerminated()} also wait for pool termination, which includes directly submitted work.
     * For a borrowed executor, only this wrapper's submitted work is tracked; direct submissions
     * are not awaited. Prefer the {@code execute(..)} methods; use this accessor to inspect or
     * to hand the same pool to another API.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AsyncExecutor asyncExecutor = new AsyncExecutor();
     * // Hand the same pool to an API that takes a plain Executor.
     * CompletableFuture.supplyAsync(() -> loadData(), asyncExecutor.getExecutor());
     * }</pre>
     *
     * @return the {@link Executor} instance used by this {@code AsyncExecutor} for executing tasks
     * @throws IllegalStateException if this {@code AsyncExecutor} has already been shut down
     */
    public synchronized Executor getExecutor() throws IllegalStateException {
        if (isShutdown) {
            throw new IllegalStateException("AsyncExecutor is shut down");
        }
        Executor result = executor;

        // This method is synchronized and isShutdown only ever changes under the same lock, so the state
        // tested above cannot move while the pool is being created: no second check is needed here.
        if (result == null) {
            final int poolIndex = POOL_INDEX.incrementAndGet();
            final AtomicInteger threadIndex = new AtomicInteger();

            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit,
                    new LinkedBlockingQueue<>(), r -> {
                        final Thread t = new Thread(r, "abacus-async-" + poolIndex + "-" + threadIndex.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    });

            final Thread hook = new Thread(() -> {
                try {
                    shutdownAndAwait(SHUTDOWN_HOOK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.warn("Error during shutdown: " + e.getMessage(), e);
                }
            }, "abacus-async-" + poolIndex + "-shutdown-hook");

            // Register the hook before publishing the pool: addShutdownHook throws once JVM shutdown has begun,
            // and a pool published first would stay installed without a hook, so the submit that failed would
            // silently succeed when retried. Nothing between the registration and the publication can throw.
            try {
                Runtime.getRuntime().addShutdownHook(hook);
                shutdownHook = hook;
            } catch (final IllegalStateException e) {
                logger.warn("JVM is already shutting down; no shutdown hook was registered for the new AsyncExecutor pool."
                        + " Its worker threads are daemon threads, so JVM exit is not blocked.", e);
            }

            executor = threadPoolExecutor;
            result = threadPoolExecutor;
        }

        // Return the local, not a second read of the volatile field: shutdown() clears it under this same
        // lock, so re-reading it could only ever give the same value at a cost.
        return result;
    }

    /**
     * Initiates an orderly shutdown of the executor used by this AsyncExecutor.
     *
     * <p>This method initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted. This method does not
     * wait for previously submitted tasks to complete execution. Use
     * {@link #shutdownAndAwait(long, TimeUnit)} to wait for task completion.</p>
     *
     * <p>If the executor is not an {@code ExecutorService} or has not been initialized,
     * this method still marks the executor as shut down to prevent future initialization.</p>
     *
     * <p><b>An externally supplied executor is never shut down.</b> When this instance was created by
     * {@link #AsyncExecutor(Executor)} it does not own the delegate, so this method only stops the
     * instance from accepting new work; the supplied executor keeps running and must be shut down by
     * whoever created it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * try {
     *     executor.execute(() -> System.out.println("task completed")).get();
     * } finally {
     *     executor.shutdown();   // gracefully shuts down even if get() fails
     * }
     * }</pre>
     *
     * @see #AsyncExecutor(Executor)
     */
    public void shutdown() {
        shutdownAndAwait(0, TimeUnit.SECONDS);
    }

    /**
     * Initiates an orderly shutdown of the executor and waits for task completion with a timeout.
     *
     * <p>This method initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted. If terminationTimeout is greater than 0,
     * the method will wait up to the specified duration for tasks to complete. If tasks are
     * still running after the timeout, the method returns without forcing termination (no
     * shutdownNow is called).</p>
     *
     * <p>If the executor is not an {@code ExecutorService} or has not been initialized, this
     * method still marks the executor as shut down to prevent future initialization.</p>
     *
     * <p><b>An externally supplied executor is never shut down.</b> For an instance created by
     * {@link #AsyncExecutor(Executor)} this method stops accepting new work and then waits, up to
     * {@code terminationTimeout}, for the tasks <i>this instance</i> submitted to finish; the supplied
     * executor itself is left running for its owner.</p>
     *
     * <p>If the calling thread is interrupted while waiting, the executor is still shut down and this method
     * returns early with the thread's interrupt status restored. A warning is logged only when the interrupt
     * arrives while it is still waiting for tasks submitted through this instance; an interrupt that arrives
     * while it is waiting for an owned pool to terminate returns silently.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor();
     * try {
     *     executor.execute(() -> System.out.println("task completed")).get();
     * } finally {
     *     executor.shutdownAndAwait(30, TimeUnit.SECONDS);   // waits up to 30 seconds
     * }
     * }</pre>
     *
     * @param terminationTimeout the maximum time to wait for executor termination; if 0 or negative,
     *                           does not wait for termination
     * @param timeUnit the time unit of the {@code terminationTimeout} argument; must not be
     *                 {@code null} when {@code terminationTimeout} is greater than 0
     * @throws IllegalArgumentException if {@code terminationTimeout} is greater than 0 and {@code timeUnit} is
     *         {@code null}.
     */
    public void shutdownAndAwait(final long terminationTimeout, final TimeUnit timeUnit) throws IllegalArgumentException {
        if (terminationTimeout > 0) {
            N.checkArgNotNull(timeUnit, cs.timeUnit);
        }
        final long timeout = terminationTimeout > 0 ? timeUnit.toNanos(terminationTimeout) : 0;
        final long started = System.nanoTime();
        final Thread hook;
        final ExecutorService pending;
        synchronized (this) {
            hook = shutdownHook;
            shutdownHook = null;
            isShutdown = true;
            if (ownsExecutor && executor instanceof ExecutorService es) {
                shutdownExecutorService = es;
            }
            executor = null;
            pending = ownsExecutor ? shutdownExecutorService : null;
        }
        // Admissions are sealed; wait outside the lifecycle lock so finishing tasks may call shutdown.
        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (final IllegalStateException ignored) {
                // The JVM is already shutting down, possibly in this hook itself.
            }
        }
        if (pending != null) {
            pending.shutdown();
        }
        if (timeout > 0) {
            awaitActiveTasks(Math.max(0, timeout - (System.nanoTime() - started)));
            final long remaining = timeout - (System.nanoTime() - started);
            if (pending != null && remaining > 0) {
                try {
                    pending.awaitTermination(remaining, TimeUnit.NANOSECONDS);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Waits up to {@code timeoutNanos} for all reservations through this instance to be released,
     * including work selected before shutdown but not yet dispatched. For a borrowed executor this is
     * the complete wrapper termination condition; an owned pool must additionally terminate.
     *
     * <p>The deadline is compared by subtraction, which stays correct even when
     * {@code System.nanoTime() + timeoutNanos} overflows - the wrap cancels in the difference, so a
     * saturated timeout such as {@code TimeUnit.SECONDS.toNanos(Long.MAX_VALUE)} still waits.</p>
     *
     * @param timeoutNanos the maximum time to wait, in nanoseconds; a value that has already elapsed
     *        returns immediately. Returns early, without throwing, if the calling thread is interrupted
     *        (the interrupt flag is restored) or the timeout expires with tasks still running.
     */
    private void awaitActiveTasks(final long timeoutNanos) {
        final long deadline = System.nanoTime() + timeoutNanos;

        while (true) {
            reclaimAbandonedTasks();

            if (activeTasks.isEmpty()) {
                return;
            }

            final long remaining = deadline - System.nanoTime();

            if (remaining <= 0) {
                return;
            }

            try {
                Thread.sleep(Math.min(10, TimeUnit.NANOSECONDS.toMillis(remaining) + 1));
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Not all AsyncExecutor tasks completed successfully before shutdown");

                return;
            }
        }
    }

    /**
     * Checks whether all tasks have completed following shutdown.
     *
     * <p>Returns {@code true} only after this wrapper has been shut down and all admitted tasks have
     * completed or dispatch has rejected them. An owned executor service must also have terminated.
     * An uninitialized or idle wrapper returns {@code false} before shutdown.</p>
     *
     * <p>A task that the delegate accepted and then never ran - discarded by a saturation policy, or dropped
     * when a borrowed pool was shut down with {@code shutdownNow()} by its owner - is released here once that
     * delegate reports termination, and is cancelled at the same time, so a task that can no longer run does
     * not pin this method to {@code false} for ever. A delegate that discards work without ever terminating
     * cannot be detected.</p>
     *
     * <p>Note that, for an initialized {@code ExecutorService}, this returns {@code true} only after
     * {@link #shutdown()} (or {@link #shutdownAndAwait(long, TimeUnit)}) has been called and all tasks have
     * completed.</p>
     *
     * <p>For an instance wrapping an externally supplied {@code ExecutorService}, termination is a
     * property of <i>this instance</i>, not of the shared delegate (which this class never shuts down):
     * it returns {@code true} once {@code shutdown()} has been called and every task submitted through
     * this instance has finished. The same wrapper accounting applies to a borrowed plain
     * {@code Executor}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * executor.shutdown();
     * if (executor.isTerminated()) {
     *     System.out.println("All tasks have completed");
     * }
     * }</pre>
     *
     * @return {@code true} if all tasks have completed following shutdown, {@code false} otherwise.
     */
    public boolean isTerminated() {
        // Outside the lifecycle lock: this asks the delegate executor for its state, and a user executor
        // must never be called with that lock held.
        reclaimAbandonedTasks();

        final ExecutorService pending;
        synchronized (this) {
            if (!isShutdown || !activeTasks.isEmpty()) {
                return false;
            }
            pending = ownsExecutor ? shutdownExecutorService : null;
        }
        return pending == null || pending.isTerminated();
    }

    /**
     * Returns a string representation of this AsyncExecutor's configuration and state.
     *
     * <p>The returned string includes configuration parameters and current state information:
     * core pool size, maximum pool size, active thread count (if the executor is a
     * ThreadPoolExecutor, otherwise "?"), keep-alive time in milliseconds, and the
     * underlying executor instance details.</p>
     *
     * <p>This method is useful for debugging and monitoring the executor's state.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AsyncExecutor executor = new AsyncExecutor(10, 20, 60L, TimeUnit.SECONDS);
     * System.out.println(executor.toString());
     * // Output: {coreThreadPoolSize: 10, maxThreadPoolSize: 20, activeCount: ?, keepAliveTime: 60000ms, Executor: null}
     * }</pre>
     *
     * @return a string representation containing the configuration and state of this AsyncExecutor.
     */
    @Override
    public String toString() {
        // Read the volatile field ONCE: a concurrent shutdown() sets it to null, and re-reading it between
        // the instanceof test and the cast would let the cast succeed on null and then NPE in getActiveCount().
        final Executor executorSnapshot = executor;
        final String activeCount = executorSnapshot instanceof ThreadPoolExecutor tpe ? "" + tpe.getActiveCount() : "?";

        return "{coreThreadPoolSize: " + coreThreadPoolSize + ", maxThreadPoolSize: " + maxThreadPoolSize + ", activeCount: " + activeCount
                + ", keepAliveTime: " + unit.toMillis(keepAliveTime) + "ms, Executor: " + N.toString(executorSnapshot) + "}";
    }
}
