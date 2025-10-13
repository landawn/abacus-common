/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Tuple.Tuple4;

/**
 * A {@code ContinuableFuture} represents an asynchronous computation that can be chained with other computations.
 * It extends the standard {@link Future} interface with additional methods for functional composition,
 * allowing developers to build complex asynchronous workflows in a fluent and readable manner.
 * 
 * <p>This class provides methods to:
 * <ul>
 *   <li>Chain asynchronous operations using {@code thenRun}, {@code thenCall}, and {@code map} methods</li>
 *   <li>Combine multiple futures using {@code runAfterBoth}, {@code callAfterBoth}, etc.</li>
 *   <li>Handle completion of either of two futures using {@code runAfterEither}, {@code callAfterEither}, etc.</li>
 *   <li>Configure execution with custom executors and delays</li>
 *   <li>Cancel operations recursively through the chain</li>
 * </ul>
 * 
 * <p><b>Key differences from CompletableFuture:</b>
 * <ul>
 *   <li>Simpler API focused on common use cases</li>
 *   <li>Recursive cancellation support via {@code cancelAll()}</li>
 *   <li>Built-in delay support via {@code thenDelay()}</li>
 *   <li>Result wrapping via {@code gett()} methods that return {@link Result} objects</li>
 * </ul>
 * 
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Simple async execution
 * ContinuableFuture<String> future = ContinuableFuture.call(() -> {
 *     Thread.sleep(1000);
 *     return "Hello World";
 * });
 * 
 * // Chaining operations
 * future.thenRun(result -> System.out.println("Result: " + result))
 *       .thenCall(() -> processNextTask())
 *       .thenDelay(2, TimeUnit.SECONDS)
 *       .thenRun(() -> System.out.println("All done!"));
 * 
 * // Combining futures
 * ContinuableFuture<Integer> future1 = ContinuableFuture.call(() -> 42);
 * ContinuableFuture<String> future2 = ContinuableFuture.call(() -> "Answer");
 * future1.callAfterBoth(future2, (num, str) -> str + ": " + num);
 * }</pre>
 *
 * @param <T> the type of the value returned by this Future's {@code get} method
 * @see Future
 * @see CompletableFuture
 * @see Futures
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html">CompletableFuture JavaDoc</a>
 * @since 0.8
 */
public class ContinuableFuture<T> implements Future<T> {

    static final Logger logger = LoggerFactory.getLogger(ContinuableFuture.class);

    final Future<T> future;

    final List<ContinuableFuture<?>> upFutures;

    final Executor asyncExecutor;

    ContinuableFuture(final Future<T> future) {
        this(future, null, null);
    }

    ContinuableFuture(final Future<T> future, final List<ContinuableFuture<?>> upFutures, final Executor asyncExecutor) {
        this.future = future;
        this.upFutures = upFutures;
        this.asyncExecutor = asyncExecutor == null ? N.ASYNC_EXECUTOR.getExecutor() : asyncExecutor;
    }

    /**
     * Executes the provided action asynchronously and returns a {@code ContinuableFuture} representing
     * the pending completion of the action. The action is executed using the default async executor.
     * 
     * <p>This method is useful for fire-and-forget operations that don't return a value.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
     *     System.out.println("Running async task");
     *     Thread.sleep(1000);
     * });
     * }</pre>
     *
     * @param action the action to be executed asynchronously
     * @return a {@code ContinuableFuture<Void>} representing the pending completion of the action
     * @see N#asyncExecute(Throwables.Runnable)
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action) {
        return run(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided action asynchronously using the specified executor and returns a
     * {@code ContinuableFuture} representing the pending completion of the action.
     * 
     * <p>This method allows you to specify a custom executor for running the action, which is
     * useful when you need specific thread pool characteristics or execution policies.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ExecutorService customExecutor = Executors.newFixedThreadPool(4);
     * ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
     *     // Heavy computation
     *     performComplexCalculation();
     * }, customExecutor);
     * }</pre>
     *
     * @param action the action to be executed asynchronously
     * @param executor the executor to use for running the action
     * @return a {@code ContinuableFuture<Void>} representing the pending completion of the action
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action, final Executor executor) {
        final FutureTask<Void> futureTask = new FutureTask<>(() -> {
            action.run();
            return null;
        });

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Executes the provided callable action asynchronously and returns a {@code ContinuableFuture}
     * representing the pending result of the action. The action is executed using the default async executor.
     * 
     * <p>This method is the primary way to start an asynchronous computation that returns a value.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     // Simulate some computation
     *     Thread.sleep(1000);
     *     return 42;
     * });
     * 
     * // Get the result (blocks until complete)
     * Integer result = future.get();
     * }</pre>
     *
     * @param <T> the type of the result returned by the callable
     * @param action the callable action to be executed asynchronously
     * @return a {@code ContinuableFuture<T>} representing the pending result of the action
     * @see N#asyncExecute(Callable)
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action) {
        return call(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided callable action asynchronously using the specified executor and returns
     * a {@code ContinuableFuture} representing the pending result of the action.
     * 
     * <p>This method allows you to specify a custom executor for running the callable, providing
     * control over thread pool characteristics and execution policies.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> {
     *     return fetchDataFromService();
     * }, scheduler);
     * }</pre>
     *
     * @param <T> the type of the result returned by the callable
     * @param action the callable action to be executed asynchronously
     * @param executor the executor to use for running the action
     * @return a {@code ContinuableFuture<T>} representing the pending result of the action
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action, final Executor executor) {
        final FutureTask<T> futureTask = new FutureTask<>(action);

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Returns a {@code ContinuableFuture} that is already completed with the provided result.
     * This is useful for creating a future that represents an immediately available value,
     * often used in testing or when converting synchronous code to asynchronous patterns.
     * 
     * <p>The returned future:
     * <ul>
     *   <li>Cannot be cancelled (returns false)</li>
     *   <li>Is always done (returns true)</li>
     *   <li>Returns the provided result immediately from get() methods</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * // Create a pre-completed future
     * ContinuableFuture<String> future = ContinuableFuture.completed("Hello");
     * 
     * // This doesn't block and returns immediately
     * String result = future.get(); // "Hello"
     * 
     * // Useful for conditional async operations
     * ContinuableFuture<Data> loadData(boolean useCache) {
     *     if (useCache && cache.contains(key)) {
     *         return ContinuableFuture.completed(cache.get(key));
     *     }
     *     return ContinuableFuture.call(() -> fetchFromDatabase(key));
     * }
     * }</pre>
     *
     * @param <T> the type of the result
     * @param result the result that the future should be completed with
     * @return a {@code ContinuableFuture} that is already completed with the provided result
     */
    public static <T> ContinuableFuture<T> completed(final T result) {
        return new ContinuableFuture<>(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public T get() {
                return result;
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) {
                return result;
            }
        }, null, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Wraps an existing {@code Future} into a {@code ContinuableFuture}, enabling the use of
     * composition and chaining methods. This is useful when integrating with APIs that return
     * standard {@code Future} objects.
     * 
     * <p>The wrapped future retains all the characteristics of the original future, including
     * its execution state, result, and cancellation behavior.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * // Working with ExecutorService that returns Future
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * Future<String> standardFuture = executor.submit(() -> "Hello");
     * 
     * // Wrap it to use ContinuableFuture features
     * ContinuableFuture<String> continuable = ContinuableFuture.wrap(standardFuture);
     * 
     * // Now can use chaining methods
     * continuable.thenRun(result -> System.out.println("Got: " + result))
     *            .thenCall(() -> processNextStep());
     * }</pre>
     *
     * @param <T> the type of the value returned by the future
     * @param future the future to wrap
     * @return a {@code ContinuableFuture} that wraps the provided future
     */
    public static <T> ContinuableFuture<T> wrap(final Future<T> future) {
        return new ContinuableFuture<>(future);
    }

    /**
     * Attempts to cancel execution of this task. This method follows the standard {@link Future#cancel(boolean)}
     * contract. If the task has already completed, has already been cancelled, or could not be
     * cancelled for some other reason, this attempt will fail.
     * 
     * <p>After this method returns, subsequent calls to {@link #isDone()} will always return {@code true}.
     * Subsequent calls to {@link #isCancelled()} will always return {@code true} if this method returned {@code true}.
     * 
     * <p><b>Note:</b> This method only cancels this future, not any upstream futures. To cancel the
     * entire chain, use {@link #cancelAll(boolean)}.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> {
     *     Thread.sleep(5000);
     *     return "Result";
     * });
     * 
     * // Cancel after 1 second
     * Thread.sleep(1000);
     * boolean cancelled = future.cancel(true); // Interrupt if running
     * }</pre>
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this task should be interrupted;
     *                              otherwise, in-progress tasks are allowed to complete
     * @return {@code false} if the task could not be cancelled, typically because it has already
     *         completed normally; {@code true} otherwise
     * @see Future#cancel(boolean)
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed normally.
     * A task that has been cancelled will never complete normally and will throw
     * a {@code CancellationException} when {@link #get()} is called.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> longRunningTask());
     * 
     * // In another thread
     * future.cancel(true);
     * 
     * if (future.isCancelled()) {
     *     System.out.println("Task was cancelled");
     * }
     * }</pre>
     *
     * @return {@code true} if this task was cancelled before it completed
     * @see Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Cancels this future and all upstream futures in the chain recursively. This method is useful
     * when you have a chain of dependent futures and want to cancel the entire computation pipeline.
     * 
     * <p>The method attempts to cancel all futures in the chain and returns {@code true} only if
     * all cancellations were successful. If any future in the chain fails to cancel, the method
     * still attempts to cancel the remaining futures.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future1 = ContinuableFuture.call(() -> fetchData());
     * ContinuableFuture<String> future2 = future1.thenCall(data -> processData(data));
     * ContinuableFuture<Void> future3 = future2.thenRun(result -> saveResult(result));
     * 
     * // This cancels future3, future2, and future1
     * boolean allCancelled = future3.cancelAll(true);
     * }</pre>
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing the tasks should be interrupted;
     *                              otherwise, in-progress tasks are allowed to complete
     * @return {@code true} if all futures in the chain were successfully cancelled; {@code false} if
     *         any future failed to cancel
     * @see #cancel(boolean)
     * @see Future#cancel(boolean)
     */
    public boolean cancelAll(final boolean mayInterruptIfRunning) {
        boolean upFuturesCancelled = true;

        if (upFutures != null && !upFutures.isEmpty()) {
            for (final ContinuableFuture<?> preFuture : upFutures) {
                upFuturesCancelled = upFuturesCancelled & preFuture.cancelAll(mayInterruptIfRunning);
            }
        }

        final boolean thisCancelled = cancel(mayInterruptIfRunning);
        return thisCancelled && upFuturesCancelled;
    }

    /**
     * Checks if this task and all upstream futures in the chain have been cancelled. This method
     * recursively checks the cancellation status of all futures in the dependency chain.
     * 
     * <p>Returns {@code true} only if every future in the chain has been cancelled. If any future
     * in the chain is not cancelled, this method returns {@code false}.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future1 = ContinuableFuture.call(() -> step1());
     * ContinuableFuture<String> future2 = future1.thenCall(data -> step2(data));
     * 
     * future2.cancelAll(true);
     * 
     * if (future2.isAllCancelled()) {
     *     System.out.println("Entire chain was cancelled");
     * }
     * }</pre>
     *
     * @return {@code true} if all futures in the chain have been cancelled; {@code false} otherwise
     * @see #isCancelled()
     * @see Future#isCancelled()
     */
    public boolean isAllCancelled() {
        if (upFutures != null && !upFutures.isEmpty()) {
            for (final ContinuableFuture<?> preFuture : upFutures) {
                if (!preFuture.isAllCancelled()) {
                    return false;
                }
            }
        }

        return isCancelled();
    }

    /**
     * Returns {@code true} if this task completed. Completion may be due to normal termination,
     * an exception, or cancellation -- in all of these cases, this method will return {@code true}.
     * 
     * <p>A completed future will never transition to any other state.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> "Done");
     * 
     * while (!future.isDone()) {
     *     System.out.println("Still processing...");
     *     Thread.sleep(100);
     * }
     * System.out.println("Task completed!");
     * }</pre>
     *
     * @return {@code true} if this task completed
     * @see Future#isDone()
     */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     * This method blocks the calling thread until the future completes.
     * 
     * <p>If the computation was cancelled, this method throws a {@code CancellationException}.
     * If the computation threw an exception, this method throws an {@code ExecutionException}
     * with the original exception as its cause.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     Thread.sleep(1000);
     *     return 42;
     * });
     * 
     * try {
     *     Integer result = future.get(); // Blocks for ~1 second
     *     System.out.println("Result: " + result);
     * } catch (InterruptedException e) {
     *     Thread.currentThread().interrupt();
     * } catch (ExecutionException e) {
     *     System.err.println("Computation failed: " + e.getCause());
     * }
     * }</pre>
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @see Future#get()
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation to complete,
     * and then retrieves its result, if available.
     * 
     * <p>This method blocks the calling thread until:
     * <ul>
     *   <li>The future completes (normally or exceptionally)</li>
     *   <li>The timeout expires</li>
     *   <li>The thread is interrupted</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> fetchFromSlowService());
     * 
     * try {
     *     // Wait maximum 5 seconds
     *     String result = future.get(5, TimeUnit.SECONDS);
     *     System.out.println("Got result: " + result);
     * } catch (TimeoutException e) {
     *     System.err.println("Operation timed out");
     *     future.cancel(true); // Cancel the operation
     * }
     * }</pre>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if the wait timed out
     * @see Future#get(long, TimeUnit)
     */
    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * Retrieves the result of the computation when it completes, wrapping both the result
     * and any exception into a {@link Result} object. This method never throws checked exceptions,
     * making it convenient for use in lambda expressions and functional chains.
     * 
     * <p>The returned {@code Result} object encapsulates either:
     * <ul>
     *   <li>The successful result of the computation</li>
     *   <li>The exception that occurred during computation or while waiting</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> riskyOperation());
     * 
     * Result<String, Exception> result = future.gett();
     * 
     * if (result.isSuccess()) {
     *     System.out.println("Success: " + result.orElseThrow());
     * } else {
     *     System.err.println("Failed: " + result.getException());
     * }
     * 
     * // Or use functional style
     * String value = result.orElse("default value");
     * }</pre>
     *
     * @return a {@code Result} object containing either the computed result or the exception
     */
    public Result<T, Exception> gett() {
        try {
            return Result.of(get(), null);
        } catch (final Exception e) {
            return Result.of(null, Futures.convertException(e));
        }
    }

    /**
     * Retrieves the result of the computation when it completes within the specified timeout,
     * wrapping both the result and any exception into a {@link Result} object. This method
     * never throws checked exceptions.
     * 
     * <p>This is the timeout version of {@link #gett()}, useful when you want to limit
     * the waiting time but still handle results in a functional style without checked exceptions.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchData());
     * 
     * Result<Data, Exception> result = future.gett(10, TimeUnit.SECONDS);
     * 
     * Data data = result.orElseGet(() -> {
     *     if (result.getException() instanceof TimeoutException) {
     *         return getCachedData(); // Fallback on timeout
     *     }
     *     return getDefaultData();
     * });
     * }</pre>
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return a {@code Result} object containing either the computed result or the exception
     */
    public Result<T, Exception> gett(final long timeout, final TimeUnit unit) {
        try {
            return Result.of(get(timeout, unit), null);
        } catch (final Exception e) {
            return Result.of(null, Futures.convertException(e));
        }
    }

    /**
     * Returns the result value if the computation is already complete, otherwise returns
     * the provided default value without blocking. This method is useful for polling
     * or providing immediate fallback values.
     * 
     * <p>Note that this method still throws exceptions if the future is done but completed
     * exceptionally. Use {@link #gett()} with {@link Result#orElseThrow()} for exception-safe
     * immediate value retrieval.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> slowComputation());
     * 
     * // Check immediately without blocking
     * String result = future.getNow("Computing...");
     * System.out.println(result); // Prints "Computing..." if not done
     * 
     * // Polling pattern
     * while (true) {
     *     String current = future.getNow(null);
     *     if (current != null) {
     *         System.out.println("Got result: " + current);
     *         break;
     *     }
     *     Thread.sleep(100);
     * }
     * }</pre>
     *
     * @param defaultValue the value to return if the computation is not yet complete
     * @return the computed result if complete, otherwise the defaultValue
     * @throws CancellationException if the computation was canceled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while checking
     */
    public T getNow(final T defaultValue) throws InterruptedException, ExecutionException {
        if (isDone()) {
            return get();
        }

        return defaultValue;
    }

    /**
     * Waits for the computation to complete and then applies the provided function to the result.
     * This method blocks until the future completes, then synchronously applies the function.
     * 
     * <p>This is a convenience method that combines {@link #get()} with function application,
     * useful for transforming results in a blocking manner.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> 42);
     * 
     * // Transform the result synchronously after completion
     * String result = future.getThenApply(num -> "The answer is: " + num);
     * System.out.println(result); // "The answer is: 42"
     * 
     * // Can throw checked exceptions
     * Data processed = future.getThenApply(num -> {
     *     if (num < 0) throw new IllegalArgumentException("Negative!");
     *     return processNumber(num); // May throw IOException
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function
     * @param <E> the type of exception the function may throw
     * @param action the function to apply to the result
     * @return the result of applying the function to the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws E if the function throws an exception
     */
    public <U, E extends Exception> U getThenApply(final Throwables.Function<? super T, ? extends U, E> action)
            throws InterruptedException, ExecutionException, E {
        return action.apply(get());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then applies
     * the provided function to the result. This method blocks until the future completes
     * or the timeout expires, then synchronously applies the function.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> fetchData());
     * 
     * try {
     *     // Wait max 5 seconds, then parse the result
     *     JsonObject json = future.getThenApply(5, TimeUnit.SECONDS, 
     *         data -> parseJson(data));
     * } catch (TimeoutException e) {
     *     // Handle timeout
     * }
     * }</pre>
     *
     * @param <U> the type of the result of the function
     * @param <E> the type of exception the function may throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the function to apply to the result
     * @return the result of applying the function to the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws E if the function throws an exception
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit, final Throwables.Function<? super T, ? extends U, E> action)
            throws InterruptedException, ExecutionException, TimeoutException, E {
        return action.apply(get(timeout, unit));
    }

    /**
     * Waits for the computation to complete and then applies the provided bi-function to both
     * the result (if successful) and any exception that occurred. This method never throws
     * the computation's exception, instead passing it to the bi-function.
     * 
     * <p>This method is useful for handling both success and failure cases in a unified way,
     * without needing try-catch blocks.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     if (Math.random() > 0.5) throw new RuntimeException("Bad luck!");
     *     return 42;
     * });
     * 
     * String message = future.getThenApply((result, exception) -> {
     *     if (exception != null) {
     *         return "Failed: " + exception.getMessage();
     *     }
     *     return "Success: " + result;
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function
     * @param <E> the type of exception the function may throw
     * @param action the bi-function to apply to the result and exception
     * @return the result of applying the function
     * @throws E if the bi-function throws an exception
     * @see #gett()
     */
    public <U, E extends Exception> U getThenApply(final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett();
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then applies
     * the provided bi-function to both the result (if successful) and any exception that occurred.
     * This method never throws the computation's exception, instead passing it to the bi-function.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchFromSlowService());
     * 
     * Response response = future.getThenApply(10, TimeUnit.SECONDS, (data, exception) -> {
     *     if (exception instanceof TimeoutException) {
     *         return Response.timeout();
     *     } else if (exception != null) {
     *         return Response.error(exception);
     *     }
     *     return Response.success(data);
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function
     * @param <E> the type of exception the function may throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the bi-function to apply to the result and exception
     * @return the result of applying the function
     * @throws E if the bi-function throws an exception
     * @see #gett(long, TimeUnit)
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett(timeout, unit);
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete and then consumes the result with the provided consumer.
     * This method blocks until the future completes, then synchronously executes the consumer.
     * 
     * <p>This is useful for side effects like logging, updating UI, or triggering other actions
     * based on the result.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> downloadFile());
     * 
     * // Process the result when ready
     * future.getThenAccept(filePath -> {
     *     System.out.println("Downloaded to: " + filePath);
     *     processFile(filePath);
     * });
     * }</pre>
     *
     * @param <E> the type of exception the consumer may throw
     * @param action the consumer to execute with the result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws E if the consumer throws an exception
     */
    public <E extends Exception> void getThenAccept(final Throwables.Consumer<? super T, E> action) throws InterruptedException, ExecutionException, E {
        action.accept(get());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then consumes
     * the result with the provided consumer. This method blocks until the future completes
     * or the timeout expires, then synchronously executes the consumer.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<List<String>> future = ContinuableFuture.call(() -> fetchLogs());
     * 
     * try {
     *     future.getThenAccept(30, TimeUnit.SECONDS, logs -> {
     *         logs.forEach(System.out::println);
     *         archiveLogs(logs);
     *     });
     * } catch (TimeoutException e) {
     *     System.err.println("Log fetch timed out");
     * }
     * }</pre>
     *
     * @param <E> the type of exception the consumer may throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the consumer to execute with the result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws E if the consumer throws an exception
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit, final Throwables.Consumer<? super T, E> action)
            throws InterruptedException, ExecutionException, TimeoutException, E {
        action.accept(get(timeout, unit));
    }

    /**
     * Waits for the computation to complete and then consumes both the result (if successful)
     * and any exception that occurred with the provided bi-consumer. This method never throws
     * the computation's exception, instead passing it to the bi-consumer.
     * 
     * <p>This method is useful for handling both success and failure cases with side effects,
     * such as logging or updating state based on the outcome.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<User> future = ContinuableFuture.call(() -> fetchUser(userId));
     * 
     * future.getThenAccept((user, exception) -> {
     *     if (exception != null) {
     *         logger.error("Failed to fetch user " + userId, exception);
     *         notifyError(exception);
     *     } else {
     *         logger.info("Fetched user: " + user.getName());
     *         updateCache(user);
     *     }
     * });
     * }</pre>
     *
     * @param <E> the type of exception the bi-consumer may throw
     * @param action the bi-consumer to execute with the result and exception
     * @throws E if the bi-consumer throws an exception
     * @see #gett()
     */
    public <E extends Exception> void getThenAccept(final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws E {
        final Result<T, Exception> result = gett();
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then consumes
     * both the result (if successful) and any exception that occurred with the provided bi-consumer.
     * This method never throws the computation's exception, instead passing it to the bi-consumer.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Config> future = ContinuableFuture.call(() -> loadConfig());
     * 
     * future.getThenAccept(5, TimeUnit.SECONDS, (config, exception) -> {
     *     if (exception instanceof TimeoutException) {
     *         useDefaultConfig();
     *     } else if (exception != null) {
     *         handleConfigError(exception);
     *     } else {
     *         applyConfig(config);
     *     }
     * });
     * }</pre>
     *
     * @param <E> the type of exception the bi-consumer may throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the bi-consumer to execute with the result and exception
     * @throws E if the bi-consumer throws an exception
     * @see #gett(long, TimeUnit)
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit,
            final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws E {
        final Result<T, Exception> result = gett(timeout, unit);
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Transforms the result of this future by applying the provided function when the future completes.
     * This method returns immediately with a new {@code ContinuableFuture} that will complete
     * with the transformed result when this future completes and the function is applied.
     * 
     * <p>This method is marked as {@code @Beta} and may be subject to change in future versions.
     * 
     * <p>If the function throws an exception, the returned future will complete exceptionally
     * with that exception wrapped in a {@code RuntimeException}.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> 21);
     * 
     * // Transform the result asynchronously
     * ContinuableFuture<String> stringFuture = future.map(num -> {
     *     return "The result is: " + (num * 2);
     * });
     * 
     * // The transformation happens when the original completes
     * System.out.println(stringFuture.get()); // "The result is: 42"
     * }</pre>
     *
     * @param <U> the type of the transformed result
     * @param func the function to apply to the result
     * @return a new {@code ContinuableFuture} with the transformed result
     */
    @Beta
    public <U> ContinuableFuture<U> map(final Throwables.Function<? super T, ? extends U, ? extends Exception> func) {
        //noinspection Convert2Diamond
        return new ContinuableFuture<>(new Future<U>() { //  java.util.concurrent.Future is abstract; cannot be instantiated
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return ContinuableFuture.this.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return ContinuableFuture.this.isCancelled();
            }

            @Override
            public boolean isDone() {
                return ContinuableFuture.this.isDone();
            }

            @Override
            public U get() throws InterruptedException, ExecutionException {
                final T ret = ContinuableFuture.this.get();

                try {
                    return func.apply(ret);
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public U get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final T ret = ContinuableFuture.this.get(timeout, unit);

                try {
                    return func.apply(ret);
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        }, null, asyncExecutor) {
            @Override
            public boolean cancelAll(final boolean mayInterruptIfRunning) {
                return ContinuableFuture.this.cancelAll(mayInterruptIfRunning);
            }

            @Override
            public boolean isAllCancelled() {
                return ContinuableFuture.this.isAllCancelled();
            }
        };
    }

    /**
     * Executes the provided action asynchronously after this future completes. The action
     * is executed using the configured executor of this future.
     * 
     * <p>This method returns immediately with a new {@code ContinuableFuture<Void>} that
     * completes when the action finishes executing. The action is only executed after
     * this future completes successfully.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture.call(() -> downloadFile())
     *     .thenRun(() -> {
     *         System.out.println("Download complete!");
     *         notifyUser();
     *     })
     *     .thenRun(() -> cleanupTempFiles());
     * }</pre>
     *
     * @param action the action to execute after this future completes
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            get();
            action.run();
            return null;
        });
    }

    /**
     * Executes the provided consumer asynchronously after this future completes, passing
     * the result to the consumer. The consumer is executed using the configured executor.
     * 
     * <p>This method returns immediately with a new {@code ContinuableFuture<Void>} that
     * completes when the consumer finishes executing. The consumer receives the result
     * of this future if it completes successfully.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture.call(() -> fetchUserData(userId))
     *     .thenRun(userData -> {
     *         updateUI(userData);
     *         saveToCache(userData);
     *     })
     *     .thenRun(() -> logCompletion());
     * }</pre>
     *
     * @param action the consumer to execute with the result
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get());
            return null;
        });
    }

    /**
     * Executes the provided bi-consumer asynchronously after this future completes, passing
     * both the result (if successful) and any exception that occurred. The bi-consumer is
     * executed using the configured executor.
     * 
     * <p>This method is useful for handling both success and failure cases in the asynchronous
     * chain without breaking the flow. The bi-consumer always executes, regardless of whether
     * this future completed normally or exceptionally.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture.call(() -> riskyOperation())
     *     .thenRun((result, exception) -> {
     *         if (exception != null) {
     *             logger.error("Operation failed", exception);
     *             sendAlert(exception);
     *         } else {
     *             logger.info("Operation succeeded: " + result);
     *             processResult(result);
     *         }
     *     })
     *     .thenRun(() -> cleanup());
     * }</pre>
     *
     * @param action the bi-consumer to execute with the result and exception
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     * @see #gett()
     */
    public ContinuableFuture<Void> thenRun(final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        });
    }

    /**
     * Executes the provided callable asynchronously after this future completes. The callable
     * is executed using the configured executor, and its result becomes the result of the
     * returned future.
     * 
     * <p>This method returns immediately with a new {@code ContinuableFuture} that completes
     * with the result of the callable. The callable is only executed after this future
     * completes successfully.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> authenticate())
     *     .thenCall(() -> {
     *         // This runs after authentication succeeds
     *         return fetchSecureData();
     *     })
     *     .thenCall(() -> processData());
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable
     * @param action the callable to execute after this future completes
     * @return a new {@code ContinuableFuture<R>} with the result of the callable
     */
    public <R> ContinuableFuture<R> thenCall(final Callable<R> action) {
        return execute(() -> {
            get();
            return action.call();
        });
    }

    /**
     * Executes the provided function asynchronously after this future completes, transforming
     * the result. The function receives the result of this future and returns a new value
     * that becomes the result of the returned future.
     * 
     * <p>This method is similar to {@link #map(Throwables.Function)} but executes asynchronously
     * in the configured executor rather than synchronously when get() is called.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(id))
     *     .thenCall(user -> {
     *         // Transform User to UserProfile
     *         return buildProfile(user);
     *     })
     *     .thenCall(profile -> enrichWithSocialData(profile));
     * }</pre>
     *
     * @param <R> the type of the result returned by the function
     * @param action the function to apply to the result
     * @return a new {@code ContinuableFuture<R>} with the transformed result
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get()));
    }

    /**
     * Executes the provided bi-function asynchronously after this future completes, transforming
     * the result based on both the value and any exception. The bi-function receives both the
     * result (if successful) and any exception that occurred.
     * 
     * <p>This method is useful for recovery scenarios where you want to provide alternative
     * values or transform exceptions into valid results.
     * The bi-function always executes, regardless of whether this future completed normally or exceptionally.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchFromPrimary())
     *     .thenCall((data, exception) -> {
     *         if (exception != null) {
     *             logger.warn("Primary failed, using fallback", exception);
     *             return fetchFromSecondary(); // Recovery
     *         }
     *         return enhanceData(data);
     *     });
     * }</pre>
     *
     * @param <R> the type of the result returned by the bi-function
     * @param action the bi-function to apply to the result and exception
     * @return a new {@code ContinuableFuture<R>} with the transformed result
     * @see #gett()
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            return action.apply(result.orElseIfFailure(null), result.getException());
        });
    }

    /**
     * Executes the provided action after both this future and the other future complete.
     * The action is executed asynchronously using the configured executor.
     * 
     * <p>The returned future completes when the action completes.
     * If either future fails, the returned future completes exceptionally with the first exception encountered.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<File> download1 = ContinuableFuture.call(() -> downloadFile1());
     * ContinuableFuture<File> download2 = ContinuableFuture.call(() -> downloadFile2());
     * 
     * download1.runAfterBoth(download2, () -> {
     *     System.out.println("Both downloads complete!");
     *     mergeFiles();
     * });
     * }</pre>
     *
     * @param other the other future to wait for
     * @param action the action to execute after both futures complete
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     */
    public ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            get();
            other.get();
            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided bi-consumer after both this future and the other future complete,
     * passing both results to the consumer. The consumer is executed asynchronously using
     * the configured executor.
     * 
     * <p>The returned future completes when the consumer completes.
     * If either future fails, the returned future completes exceptionally with the first exception encountered.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(id));
     * ContinuableFuture<Profile> profileFuture = ContinuableFuture.call(() -> fetchProfile(id));
     * 
     * userFuture.runAfterBoth(profileFuture, (user, profile) -> {
     *     mergeUserAndProfile(user, profile);
     *     updateCache(user, profile);
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result
     * @param other the other future to wait for
     * @param action the bi-consumer to execute with both results
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     */
    public <U> ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiConsumer<? super T, ? super U, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get(), other.get());
            return null;
        }, other);
    }

    /**
     * Executes the provided consumer after both this future and the other future complete,
     * passing a {@link Tuple4} containing both results and any exceptions to the consumer.
     * The consumer is executed regardless of whether the futures completed successfully.
     * 
     * <p>The tuple contains: (result1, exception1, result2, exception2) where:
     * <ul>
     *   <li>result1/exception1 are from this future</li>
     *   <li>result2/exception2 are from the other future</li>
     *   <li>If a future succeeds, its result is non-null and exception is null</li>
     *   <li>If a future fails, its result is null and exception is non-null</li>
     * </ul>
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> primary = ContinuableFuture.call(() -> fetchPrimary());
     * ContinuableFuture<Data> backup = ContinuableFuture.call(() -> fetchBackup());
     * 
     * primary.runAfterBoth(backup, tuple -> {
     *     if (tuple._2 == null) {  // primary succeeded
     *         processData(tuple._1);
     *     } else if (tuple._4 == null) {  // backup succeeded
     *         processData(tuple._3);
     *     } else {
     *         handleBothFailed(tuple._2, tuple._4);
     *     }
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result
     * @param other the other future to wait for
     * @param action the consumer to execute with the tuple of results and exceptions
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     * @see #gett()
     */
    public <U> ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<U> other,
            final Throwables.Consumer<? super Tuple4<T, ? super Exception, U, ? super Exception>, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            final Result<U, Exception> result2 = other.gett();

            action.accept(Tuple.of(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException()));
            return null;
        }, other);
    }

    /**
     * Executes the provided quad-consumer after both this future and the other future complete,
     * passing all four values (both results and exceptions) as separate parameters. The consumer
     * is executed regardless of whether the futures completed successfully.
     * 
     * <p>This method provides the same functionality as
     * {@link #runAfterBoth(ContinuableFuture, Throwables.Consumer)} but with individual parameters
     * instead of a tuple.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Order> orderFuture = ContinuableFuture.call(() -> createOrder());
     * ContinuableFuture<Payment> paymentFuture = ContinuableFuture.call(() -> processPayment());
     * 
     * orderFuture.runAfterBoth(paymentFuture, (order, orderEx, payment, paymentEx) -> {
     *     if (orderEx != null || paymentEx != null) {
     *         rollbackTransaction(order, payment, orderEx, paymentEx);
     *     } else {
     *         confirmTransaction(order, payment);
     *     }
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result
     * @param other the other future to wait for
     * @param action the quad-consumer to execute with both results and exceptions
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action
     * @see #gett()
     */
    public <U> ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<U> other,
            final Throwables.QuadConsumer<? super T, ? super Exception, ? super U, ? super Exception, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            final Result<U, Exception> result2 = other.gett();

            action.accept(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided callable after both this future and the other future complete successfully.
     * The callable is executed asynchronously using the configured executor.
     * 
     * <p>The returned future completes with the result of the callable. If either input
     * future fails, the returned future also fails without executing the callable.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Config> configFuture = ContinuableFuture.call(() -> loadConfig());
     * ContinuableFuture<Database> dbFuture = ContinuableFuture.call(() -> connectDB());
     * 
     * ContinuableFuture<Service> serviceFuture = configFuture.callAfterBoth(dbFuture, () -> {
     *     // Both config and database are ready
     *     return initializeService();
     * });
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable
     * @param other the other future to wait for completion; must not be null
     * @param action the callable to execute after both futures complete; must not be null
     * @return a new {@code ContinuableFuture<R>} that completes with the result of the callable
     */
    public <R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<?> other, final Callable<R> action) {
        return execute(() -> {
            get();
            other.get();
            return action.call();
        }, other);
    }

    /**
     * Executes the provided BiFunction after both this ContinuableFuture and the other ContinuableFuture complete successfully.
     * The BiFunction receives the results of both futures as input parameters and returns a result.
     * 
     * <p>This method enables combining the results of two independent asynchronous computations.
     * If either future fails, the returned future completes exceptionally with the first exception encountered.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(userId));
     * ContinuableFuture<Settings> settingsFuture = ContinuableFuture.call(() -> fetchSettings());
     * 
     * ContinuableFuture<Profile> profileFuture = userFuture.callAfterBoth(settingsFuture, 
     *     (user, settings) -> createProfile(user, settings));
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture
     * @param <R> the result type of the BiFunction and the returned ContinuableFuture
     * @param other the other ContinuableFuture that must complete before executing the action; must not be null
     * @param action the BiFunction to execute with both results; must not be null
     * @return a new ContinuableFuture that completes with the result of the BiFunction
     */
    public <U, R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get(), other.get()), other);
    }

    /**
     * Executes the provided function after both this ContinuableFuture and the other ContinuableFuture complete,
     * regardless of whether they complete successfully or exceptionally.
     * The function receives a {@link Tuple4} containing both results and their exceptions (if any).
     * 
     * <p>This method is useful when you need to handle the results of both futures regardless of their success/failure status.
     * The tuple contains: (result1, exception1, result2, exception2) where results are null if the corresponding
     * future failed, and exceptions are null if the corresponding future succeeded.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> primaryFuture = ContinuableFuture.call(() -> fetchPrimaryData());
     * ContinuableFuture<Data> backupFuture = ContinuableFuture.call(() -> fetchBackupData());
     * 
     * ContinuableFuture<Data> result = primaryFuture.callAfterBoth(backupFuture, tuple -> {
     *     Data primary = tuple._1;
     *     Exception primaryError = tuple._2;
     *     Data backup = tuple._3;
     *     Exception backupError = tuple._4;
     *     
     *     if (primary != null) return primary;
     *     if (backup != null) return backup;
     *     throw new DataUnavailableException("Both sources failed");
     * });
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture
     * @param <R> the result type of the function and the returned ContinuableFuture
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the function that processes the tuple of results and exceptions; must not be null
     * @return a new ContinuableFuture that completes with the result of the function
     * @see #gett()
     */
    public <U, R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<U> other,
            final Throwables.Function<? super Tuple4<T, ? super Exception, U, ? super Exception>, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            final Result<U, Exception> result2 = other.gett();

            return action.apply(Tuple.of(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException()));
        }, other);
    }

    /**
     * Executes the provided QuadFunction after both this ContinuableFuture and the other ContinuableFuture complete,
     * regardless of whether they complete successfully or exceptionally.
     * The function receives four parameters: the results and exceptions from both futures.
     * 
     * <p>This method provides maximum flexibility for handling the completion of two futures,
     * allowing custom logic based on any combination of success and failure states.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Response> apiFuture = ContinuableFuture.call(() -> callAPI());
     * ContinuableFuture<Cache> cacheFuture = ContinuableFuture.call(() -> loadCache());
     * 
     * ContinuableFuture<Result> combined = apiFuture.callAfterBoth(cacheFuture,
     *     (apiResponse, apiError, cacheData, cacheError) -> {
     *         if (apiError == null && apiResponse.isValid()) {
     *             return Result.fromApi(apiResponse);
     *         } else if (cacheError == null) {
     *             return Result.fromCache(cacheData);
     *         } else {
     *             throw new ServiceUnavailableException("Both API and cache failed");
     *         }
     *     });
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture
     * @param <R> the result type of the QuadFunction and the returned ContinuableFuture
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the QuadFunction that processes both results and exceptions; must not be null
     * @return a new ContinuableFuture that completes with the result of the QuadFunction
     */
    public <U, R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<U> other,
            final Throwables.QuadFunction<? super T, ? super Exception, ? super U, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            final Result<U, Exception> result2 = other.gett();

            return action.apply(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException());
        }, other);
    }

    /**
     * Executes the provided Runnable action after either this ContinuableFuture or the other ContinuableFuture completes
     * (successfully or exceptionally).
     * 
     * <p>This method is useful for triggering an action as soon as any of the futures completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> primarySource = ContinuableFuture.call(() -> fetchFromPrimary());
     * ContinuableFuture<Data> secondarySource = ContinuableFuture.call(() -> fetchFromSecondary());
     * 
     * primarySource.runAfterEither(secondarySource, () -> {
     *     System.out.println("At least one data source has responded");
     *     notifyDataAvailable();
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the Runnable to execute after either future completes; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            Futures.iterate(Array.asList(ContinuableFuture.this, other), r -> r).next();

            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided Consumer action after either this ContinuableFuture or the other ContinuableFuture completes
     * (successfully or exceptionally). The Consumer receives the result of whichever future completes first.
     * If the future that completes first fails, the consumer receives null.
     * 
     * <p>This method is useful for triggering an action as soon as any of the futures completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Weather> localWeather = ContinuableFuture.call(() -> getLocalWeather());
     * ContinuableFuture<Weather> remoteWeather = ContinuableFuture.call(() -> getRemoteWeather());
     * 
     * localWeather.runAfterEither(remoteWeather, weather -> {
     *     displayWeather(weather); // weather may be null if the first future failed
     *     logSource(weather.getSource());
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the Consumer to execute with the first available result; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> ret = Futures.iterate(Array.asList(ContinuableFuture.this, other), r -> r).next();

            action.accept(ret.orElseIfFailure(null));
            return null;
        }, other);
    }

    /**
     * Executes the provided Consumer action after either this ContinuableFuture or the other ContinuableFuture completes
     * (successfully or exceptionally).The BiConsumer receives both the result (if successful)
     * and the exception (if failed) from whichever future completes first.
     * 
     * <p>This method is useful for triggering an action as soon as any of the futures completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Config> localConfig = ContinuableFuture.call(() -> loadLocalConfig());
     * ContinuableFuture<Config> remoteConfig = ContinuableFuture.call(() -> loadRemoteConfig());
     * 
     * localConfig.runAfterEither(remoteConfig, (config, error) -> {
     *     if (error != null) {
     *         logger.warn("Config loading failed: " + error.getMessage());
     *         useDefaultConfig();
     *     } else {
     *         applyConfig(config);
     *     }
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the BiConsumer to execute with the result and exception; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     * @see #gett()
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).gett();

            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided Callable action after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally).
     * 
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Connection> primary = ContinuableFuture.call(() -> connectToPrimary());
     * ContinuableFuture<Connection> backup = ContinuableFuture.call(() -> connectToBackup());
     * 
     * ContinuableFuture<Session> session = primary.callAfterEither(backup, () -> {
     *     // Create session as soon as any connection is established
     *     return createNewSession();
     * });
     * }</pre>
     *
     * @param <R> the result type of the callable and the returned ContinuableFuture
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the Callable to execute after either future completes; must not be null
     * @return a new ContinuableFuture that completes with the result of the callable
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<?> other, final Callable<? extends R> action) {
        return execute(() -> {
            Futures.iterate(Array.asList(ContinuableFuture.this, other), r -> r).next();

            return action.call();
        }, other);
    }

    /**
     * Executes the provided function after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally). The function transforms the result of whichever future completes first.
     * If the future that completes first fails, the function receives null.
     * 
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Price> vendorA = ContinuableFuture.call(() -> getPriceFromVendorA());
     * ContinuableFuture<Price> vendorB = ContinuableFuture.call(() -> getPriceFromVendorB());
     * 
     * ContinuableFuture<Order> order = vendorA.callAfterEither(vendorB, price -> {
     *     // Process the first available price
     *     return createOrder(price); // price may be null if the first future failed
     * });
     * }</pre>
     *
     * @param <R> the result type of the function and the returned ContinuableFuture
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the function to transform the first available result; must not be null
     * @return a new ContinuableFuture that completes with the transformed result
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> ret = Futures.iterate(Array.asList(ContinuableFuture.this, other), r -> r).next();

            return action.apply(ret.orElseIfFailure(null));
        }, other);
    }

    /**
     * Executes the provided BiFunction after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally). The BiFunction receives both the result (if successful)
     * and the exception (if failed) from whichever future completes first, and returns a transformed result.
     * 
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Data> fastSource = ContinuableFuture.call(() -> fetchFromFastSource());
     * ContinuableFuture<Data> slowSource = ContinuableFuture.call(() -> fetchFromSlowSource());
     * 
     * ContinuableFuture<ProcessedData> result = fastSource.callAfterEither(slowSource, 
     *     (data, error) -> {
     *         if (error != null) {
     *             return ProcessedData.empty();
     *         }
     *         return processData(data);
     *     });
     * }</pre>
     *
     * @param <R> the result type of the BiFunction and the returned ContinuableFuture
     * @param other the other ContinuableFuture to race against; must not be null
     * @param action the BiFunction to transform the result and exception; must not be null
     * @return a new ContinuableFuture that completes with the transformed result
     * @see #gett()
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> ret = Futures.iterate(Array.asList(ContinuableFuture.this, other), r -> r).next();

            return action.apply(ret.orElseIfFailure(null), ret.getException());
        }, other);
    }

    /**
     * Executes the provided Runnable action after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. If both futures fail, the action is not executed and the returned
     * future completes exceptionally with the first exception.
     * 
     * <p>This method waits for at least one successful completion before executing the action,
     * making it useful when you need a successful result from at least one source.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Void> saveToDatabase = ContinuableFuture.run(() -> saveToDb());
     * ContinuableFuture<Void> saveToCache = ContinuableFuture.run(() -> saveToCache());
     * 
     * saveToDatabase.runAfterFirstSucceed(saveToCache, () -> {
     *     // Execute only after at least one save operation succeeds
     *     notifySaveComplete();
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the Runnable to execute after the first successful completion; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     */
    public ContinuableFuture<Void> runAfterFirstSucceed(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            final ObjIterator<Result<Object, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<Object, Exception> firstResult = iter.next();

            if (firstResult.isFailure()) {
                final Result<Object, Exception> secondResult = iter.next();

                if (secondResult.isFailure()) {
                    throw firstResult.getException();
                }
            }

            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided Consumer action after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The Consumer receives the result of the first future that completes successfully.
     * 
     * <p>If both futures fail, the action is not executed and the returned future completes exceptionally
     * with the exception from the first future that completed.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<User> dbUser = ContinuableFuture.call(() -> loadUserFromDatabase(id));
     * ContinuableFuture<User> cacheUser = ContinuableFuture.call(() -> loadUserFromCache(id));
     * 
     * dbUser.runAfterFirstSucceed(cacheUser, user -> {
     *     // Process the first successfully loaded user
     *     updateLastAccessed(user);
     *     notifyUserLoaded(user);
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the Consumer to execute with the first successful result; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     */
    public ContinuableFuture<Void> runAfterFirstSucceed(final ContinuableFuture<? extends T> other,
            final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            final ObjIterator<Result<T, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<T, Exception> firstResult = iter.next();
            T ret = null;

            if (firstResult.isFailure()) {
                final Result<T, Exception> secondResult = iter.next();

                if (secondResult.isFailure()) {
                    throw firstResult.getException();
                } else {
                    ret = secondResult.orElseIfFailure(null);
                }
            } else {
                ret = firstResult.orElseIfFailure(null);
            }

            action.accept(ret);

            return null;
        }, other);
    }

    /**
     * Executes the provided BiConsumer action after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The BiConsumer receives the result and exception, where at least one will be non-null.
     * 
     * <p>If the first future to complete succeeds, the BiConsumer receives (result, null).
     * If both futures fail, the BiConsumer receives (null, firstException).
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Config> primaryConfig = ContinuableFuture.call(() -> loadPrimaryConfig());
     * ContinuableFuture<Config> fallbackConfig = ContinuableFuture.call(() -> loadFallbackConfig());
     * 
     * primaryConfig.runAfterFirstSucceed(fallbackConfig, (config, error) -> {
     *     if (config != null) {
     *         applyConfig(config);
     *     } else {
     *         logger.error("All config sources failed", error);
     *         useHardcodedDefaults();
     *     }
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the BiConsumer to execute with the result and exception; must not be null
     * @return a new ContinuableFuture<Void> that completes after executing the action
     */
    @Beta
    public ContinuableFuture<Void> runAfterFirstSucceed(final ContinuableFuture<? extends T> other,
            final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) {
        return execute(() -> {
            final ObjIterator<Result<T, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<T, Exception> firstResult = iter.next();
            Result<T, Exception> ret = null;

            if (firstResult.isFailure()) {
                final Result<T, Exception> secondResult = iter.next();

                if (secondResult.isSuccess()) {
                    ret = secondResult;
                }
            }

            if (ret == null) {
                ret = firstResult;
            }

            action.accept(ret.orElseIfFailure(null), ret.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided Callable action after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. If both futures fail, the callable is not executed and the returned
     * future completes exceptionally with the first exception.
     * 
     * <p>This method ensures that the callable is only executed if at least one of the futures succeeds,
     * making it useful for dependent operations that require a successful prerequisite.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Auth> oauth = ContinuableFuture.call(() -> authenticateOAuth());
     * ContinuableFuture<Auth> apiKey = ContinuableFuture.call(() -> authenticateApiKey());
     * 
     * ContinuableFuture<Session> session = oauth.callAfterFirstSucceed(apiKey, () -> {
     *     // Create session only after successful authentication
     *     return createUserSession();
     * });
     * }</pre>
     *
     * @param <R> the result type of the callable and the returned ContinuableFuture
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the Callable to execute after the first successful completion; must not be null
     * @return a new ContinuableFuture that completes with the result of the callable
     */
    public <R> ContinuableFuture<R> callAfterFirstSucceed(final ContinuableFuture<?> other, final Callable<? extends R> action) {
        return execute(() -> {
            final ObjIterator<Result<Object, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<Object, Exception> firstResult = iter.next();

            if (firstResult.isFailure()) {
                final Result<Object, Exception> secondResult = iter.next();

                if (secondResult.isFailure()) {
                    throw firstResult.getException();
                }
            }

            return action.call();
        }, other);
    }

    /**
     * Executes the provided function after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The function transforms the result of the first future that completes successfully.
     * 
     * <p>If both futures fail, the function is not executed and the returned future completes exceptionally
     * with the exception from the first future that completed.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<RawData> primarySource = ContinuableFuture.call(() -> fetchFromPrimary());
     * ContinuableFuture<RawData> backupSource = ContinuableFuture.call(() -> fetchFromBackup());
     * 
     * ContinuableFuture<ProcessedData> processed = primarySource.callAfterFirstSucceed(backupSource, 
     *     rawData -> {
     *         // Transform the first successfully fetched data
     *         return processAndValidate(rawData);
     *     });
     * }</pre>
     *
     * @param <R> the result type of the function and the returned ContinuableFuture
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the function to transform the first successful result; must not be null
     * @return a new ContinuableFuture that completes with the transformed result
     */
    public <R> ContinuableFuture<R> callAfterFirstSucceed(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final ObjIterator<Result<T, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<T, Exception> firstResult = iter.next();
            T ret = null;

            if (firstResult.isFailure()) {
                final Result<T, Exception> secondResult = iter.next();

                if (secondResult.isFailure()) {
                    throw firstResult.getException();
                } else {
                    ret = secondResult.orElseIfFailure(null);
                }
            } else {
                ret = firstResult.orElseIfFailure(null);
            }

            return action.apply(ret);
        }, other);
    }

    /**
     * Executes the provided BiFunction after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The BiFunction receives the result and exception, where at least one will be non-null.
     * 
     * <p>If the first future to complete succeeds, the BiFunction receives (result, null).
     * If both futures fail, the BiFunction receives (null, firstException).
     * This allows the BiFunction to handle both cases and produce an appropriate result.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture<Price> vendorPrice = ContinuableFuture.call(() -> getVendorPrice());
     * ContinuableFuture<Price> marketPrice = ContinuableFuture.call(() -> getMarketPrice());
     * 
     * ContinuableFuture<Quote> quote = vendorPrice.callAfterFirstSucceed(marketPrice,
     *     (price, error) -> {
     *         if (price != null) {
     *             return Quote.withPrice(price);
     *         } else {
     *             // Both sources failed, return quote with error status
     *             return Quote.unavailable(error.getMessage());
     *         }
     *     });
     * }</pre>
     *
     * @param <R> the result type of the BiFunction and the returned ContinuableFuture
     * @param other the other ContinuableFuture to wait for; must not be null
     * @param action the BiFunction to transform based on result and exception; must not be null
     * @return a new ContinuableFuture that completes with the transformed result
     * @see #gett()
     */
    public <R> ContinuableFuture<R> callAfterFirstSucceed(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final ObjIterator<Result<T, Exception>> iter = Futures.iterate(Arrays.asList(ContinuableFuture.this, other), Fn.identity());
            final Result<T, Exception> firstResult = iter.next();
            Result<T, Exception> ret = null;

            if (firstResult.isFailure()) {
                final Result<T, Exception> secondResult = iter.next();

                if (secondResult.isSuccess()) {
                    ret = secondResult;
                }
            }

            if (ret == null) {
                ret = firstResult;
            }

            return action.apply(ret.orElseIfFailure(null), ret.getException());
        }, other);
    }

    //    /**

    private <R> ContinuableFuture<R> execute(final Callable<R> command) {
        return execute(command, null);
    }

    private <R> ContinuableFuture<R> execute(final Callable<R> command, final ContinuableFuture<?> other) {
        return execute(new FutureTask<>(command), other);
    }

    private <U> ContinuableFuture<U> execute(final FutureTask<U> futureTask, final ContinuableFuture<?> other) {
        asyncExecutor.execute(futureTask);

        @SuppressWarnings("rawtypes")
        final List<ContinuableFuture<?>> upFutureList = other == null ? (List) List.of(this) : Arrays.asList(this, other);
        return new ContinuableFuture<>(futureTask, upFutureList, asyncExecutor);
    }

    /**
     * Configures this ContinuableFuture to delay the execution of the next chained action.
     * The delay is applied before the next action in the chain is executed.
     * 
     * <p>This method is useful for implementing timeouts, rate limiting, or introducing
     * deliberate pauses in asynchronous workflows.
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ContinuableFuture.call(() -> sendRequest())
     *     .thenDelay(2, TimeUnit.SECONDS)  // Wait 2 seconds after request completes
     *     .thenCall(() -> checkResponse())
     *     .thenDelay(1, TimeUnit.SECONDS)  // Wait 1 second before final action
     *     .thenRun(() -> processResults());
     * }</pre>
     *
     * @param delay the delay duration before the next action is executed; must be non-negative
     * @param unit the time unit of the delay parameter; must not be null
     * @return a new ContinuableFuture configured with the specified delay if delay > 0, or this future if delay <= 0
     */
    public ContinuableFuture<T> thenDelay(final long delay, final TimeUnit unit) {
        if (delay <= 0) {
            return this;
        }

        return with(asyncExecutor, delay, unit);
    }

    /**
     * Configures this ContinuableFuture to execute the next chained action using the specified executor.
     * This allows changing the execution context for subsequent operations in the chain.
     * 
     * <p>This method is useful when different parts of an asynchronous workflow need to run
     * on different thread pools (e.g., I/O operations on an I/O pool, CPU-intensive work on
     * a computation pool).
     * 
     * <p><b>Example:</b>
     * <pre>{@code
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * ExecutorService cpuExecutor = Executors.newFixedThreadPool(4);
     * 
     * ContinuableFuture.call(() -> readFromFile())          // Runs on default executor
     *     .thenUse(cpuExecutor)                             // Switch to CPU executor
     *     .thenCall(() -> processData())                    // CPU-intensive processing
     *     .thenUse(ioExecutor)                              // Switch to I/O executor
     *     .thenRun(result -> writeToFile(result));          // I/O operation
     * }</pre>
     *
     * @param executor the executor to use for subsequent actions in the chain; must not be null
     * @return a new ContinuableFuture configured with the specified executor
     * @throws IllegalArgumentException if {@code executor} is null
     */
    public ContinuableFuture<T> thenUse(final Executor executor) {
        return with(executor, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Internal method that creates a new ContinuableFuture with the specified executor and delay configuration.
     * This method combines the functionality of thenDelay and thenUse.
     *
     * @param executor the executor to use for subsequent operations; must not be null
     * @param delay the delay before executing subsequent operations
     * @param unit the time unit for the delay
     * @return a new ContinuableFuture with the specified configuration
     * @deprecated This is an internal method and should not be used directly. Use {@link #thenDelay} or {@link #thenUse} instead.
     */
    @Deprecated
    ContinuableFuture<T> with(final Executor executor, final long delay, final TimeUnit unit) {
        N.checkArgNotNull(executor);

        //noinspection Convert2Diamond
        return new ContinuableFuture<>(new Future<T>() { //  java.util.concurrent.Future is abstract; cannot be instantiated
            private final long delayInMillis = unit.toMillis(delay);
            private final long startTime = System.currentTimeMillis();
            private volatile boolean isDelayed = false;

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                final boolean isDone = future.isDone();

                if (isDone) {
                    delay();
                }

                return isDone;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                delay();

                return future.get();
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                delay();

                return future.get(timeout, unit);
            }

            private void delay() {
                if (!isDelayed) {
                    synchronized (this) {
                        if (!isDelayed) {
                            isDelayed = true;
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            long remainingDelay = delayInMillis - elapsedTime;
                            if (remainingDelay > 0) {
                                N.sleepUninterruptibly(remainingDelay);
                            }
                        }
                    }
                }
            }
        }, null, executor) {
            @Override
            public boolean cancelAll(final boolean mayInterruptIfRunning) { //NOSONAR
                return super.cancelAll(mayInterruptIfRunning);
            }

            @Override
            public boolean isAllCancelled() { //NOSONAR
                return super.isAllCancelled();
            }
        };
    }

    // https://stackoverflow.com/questions/23301598/transform-java-future-into-a-completablefuture

}
