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
 * The ContinuableFuture class represents a computation that may be asynchronous. It implements the Future interface.
 * This class provides methods to check if the computation is complete, to wait for its completion, and to retrieve the result of the computation.
 * The result can only be retrieved using method get when the computation has completed, blocking if necessary until it is ready.
 * Cancellation is performed by the cancel method. Additional methods are provided to determine if the task completed normally or was cancelled.
 * Once a computation has completed, the computation cannot be cancelled. If you would like to use a Future for the sake
 * of cancellability but not provide a usable result, you can declare types of the form Future<?> and return {@code null} as a result of the underlying task.
 *
 * @param <T> the result type returned by this Future's get method
 * @see Future
 * @see CompletableFuture
 * @see Futures
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html</a>
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
     * Executes the provided action asynchronously.
     *
     * @param action The action to be executed asynchronously.
     * @return A ContinuableFuture representing pending completion of the action.
     * @see N#asyncExecute(Throwables.Runnable)
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action) {
        return run(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided action asynchronously using the specified executor.
     *
     * @param action The action to be executed asynchronously.
     * @param executor The executor to run the action.
     * @return A ContinuableFuture representing pending completion of the action.
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
     * Executes the provided action asynchronously and returns a ContinuableFuture representing the pending completion of the action.
     *
     * @param <T> The result type of the callable action.
     * @param action The callable action to be executed asynchronously.
     * @return A ContinuableFuture representing pending completion of the action.
     * @see N#asyncExecute(Callable)
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action) {
        return call(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided action asynchronously using the specified executor and returns a ContinuableFuture representing the pending completion of the action.
     *
     * @param <T> The result type of the callable action.
     * @param action The callable action to be executed asynchronously.
     * @param executor The executor to run the action.
     * @return A ContinuableFuture representing pending completion of the action.
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action, final Executor executor) {
        final FutureTask<T> futureTask = new FutureTask<>(action);

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Returns a ContinuableFuture that is already completed with the provided result.
     *
     * @param <T> The result type of the ContinuableFuture.
     * @param result The result that the ContinuableFuture should be completed with.
     * @return A ContinuableFuture that is already completed with the provided result.
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
     * Wraps the provided Future into a ContinuableFuture.
     *
     * @param <T> The result type returned by this Future's get method.
     * @param future The future to be wrapped into a ContinuableFuture.
     * @return A ContinuableFuture that wraps the provided Future.
     */
    public static <T> ContinuableFuture<T> wrap(final Future<T> future) {
        return new ContinuableFuture<>(future);
    }

    /**
     * Attempts to cancel execution of this task. This attempt will fail if the task has already completed, has already been cancelled, or could not be cancelled for some other reason.
     * If successful, and this task has not started when cancel is called, this task should never run.
     * If the task has already started, then the mayInterruptIfRunning parameter determines whether the thread executing this task should be interrupted in an attempt to stop the task.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete.
     * @return {@code false} if the task could not be cancelled, typically because it has already completed normally; {@code true} otherwise.
     * @see Future#cancel(boolean)
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Checks if this task has been cancelled.
     *
     * @return {@code true} if this task has been cancelled, {@code false} otherwise.
     * @see Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Cancel this future and all the previous stage future recursively.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete.
     * @return {@code true} if all tasks were cancelled successfully; {@code false} otherwise.
     * @see #cancel(boolean)
     * @see Future#cancel(boolean)
     */
    public boolean cancelAll(final boolean mayInterruptIfRunning) {
        boolean res = true;

        if (N.notEmpty(upFutures)) {
            for (final ContinuableFuture<?> preFuture : upFutures) {
                res = res & preFuture.cancelAll(mayInterruptIfRunning); //NOSONAR
            }
        }

        return cancel(mayInterruptIfRunning) && res;
    }

    /**
     * Checks if this task and all the previous stage futures have been cancelled recursively.
     *
     * @return {@code true} if all tasks have been cancelled, {@code false} otherwise.
     * @see #isCancelled()
     * @see Future#isCancelled()
     */
    public boolean isAllCancelled() {
        if (N.notEmpty(upFutures)) {
            for (final ContinuableFuture<?> preFuture : upFutures) {
                if (!preFuture.isAllCancelled()) {
                    return false;
                }
            }
        }

        return isCancelled();
    }

    /**
     * Checks if this task has completed.
     *
     * @return {@code true} if this task has completed, {@code false} otherwise.
     * @see Future#isDone()
     */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until it is ready.
     *
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @see Future#get()
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @see Future#get(long, TimeUnit)
     */
    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * Retrieves the result of the computation when it's ready, without throwing checked exceptions.
     * This method wraps the result and any exception that occurred during the computation into a Result object.
     *
     * @return a Result object containing the computed result or the exception if one occurred.
     */
    public Result<T, Exception> gett() {
        try {
            return Result.of(get(), null);
        } catch (final Exception e) {
            return Result.of(null, e);
        }
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     * This method wraps the result and any exception that occurred during the computation into a Result object.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return a Result object containing the computed result or the exception if one occurred.
     */
    public Result<T, Exception> gett(final long timeout, final TimeUnit unit) {
        try {
            return Result.of(get(timeout, unit), null);
        } catch (final Exception e) {
            return Result.of(null, e);
        }
    }

    /**
     * Retrieves the result of the computation immediately if it's ready, otherwise returns the provided default value.
     *
     * @param defaultValue the value to be returned if the computation is not yet ready
     * @return the computed result if it's ready, or the defaultValue otherwise
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    public T getNow(final T defaultValue) throws InterruptedException, ExecutionException {
        if (isDone()) {
            return get();
        }

        return defaultValue;
    }

    /**
     * Retrieves the result of the computation when it's ready, applies the provided function to it, and returns the function's result.
     * This method blocks if necessary until the computation is ready.
     *
     * @param <U> the result type of the function
     * @param <E> the type of the exception that the function can throw
     * @param action the function to apply to the computed result
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
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     * Then applies the provided function to the result and returns the function's result.
     *
     * @param <U> the result type of the function
     * @param <E> the type of the exception that the function can throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the function to apply to the computed result
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
     * Retrieves the result of the computation when it's ready, applies the provided function to it, and returns the function's result.
     * This method blocks if necessary until the computation is ready.
     *
     * @param <U> the result type of the function
     * @param <E> the type of the exception that the function can throw
     * @param action the function to apply to the computed result
     * @return the result of applying the function to the computed result
     * @throws E if the function throws an exception
     * @see #gett()
     */
    public <U, E extends Exception> U getThenApply(final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett();
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     * Then applies the provided function to the result and returns the function's result.
     *
     * @param <U> the result type of the function
     * @param <E> the type of the exception that the function can throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the function to apply to the computed result
     * @return the result of applying the function to the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws E if the function throws an exception
     * @see #gett(long, TimeUnit)
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett(timeout, unit);
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Retrieves the result of the computation when it's ready, applies the provided consumer to it.
     * This method blocks if necessary until the computation is ready.
     *
     * @param <E> the type of the exception that the consumer can throw
     * @param action the consumer to apply to the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws E if the consumer throws an exception
     */
    public <E extends Exception> void getThenAccept(final Throwables.Consumer<? super T, E> action) throws InterruptedException, ExecutionException, E {
        action.accept(get());
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     * Then applies the provided consumer to the result.
     *
     * @param <E> the type of the exception that the consumer can throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the consumer to apply to the computed result
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
     * Retrieves the result of the computation when it's ready, applies the provided bi-consumer to it.
     * This method blocks if necessary until the computation is ready.
     *
     * @param <E> the type of the exception that the bi-consumer can throw
     * @param action the bi-consumer to apply to the computed result and any exception that occurred during the computation
     * @throws E if the bi-consumer throws an exception
     * @see #gett()
     */
    public <E extends Exception> void getThenAccept(final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws E {
        final Result<T, Exception> result = gett();
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Retrieves the result of the computation when it's ready, blocking if necessary until the specified timeout expires.
     * Then applies the provided consumer to the result.
     *
     * @param <E> the type of the exception that the consumer can throw
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param action the consumer to apply to the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     * @throws E if the consumer throws an exception
     * @see #gett(long, TimeUnit)
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit,
            final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws E {
        final Result<T, Exception> result = gett(timeout, unit);
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    //    public void complete() throws InterruptedException, ExecutionException {
    //        get();
    //    }
    //
    //    public void complete(Consumer<? super T> action) {
    //        try {
    //            action.accept(get());
    //        } catch (InterruptedException | ExecutionException e) {
    //            throw ExceptionUtil.toRuntimeException(e, true);
    //        }
    //    }
    //
    //    public void complete(BiConsumer<? super T, ? super Exception> action) {
    //        final Result<T, Exception> result = gett();
    //        action.accept(result.orElse(null), result.getException());
    //    }

    /**
     * Transforms the result of the computation when it's ready, by applying the provided function to it.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <U> the result type of the function, and of the returned ContinuableFuture
     * @param func the function to apply to the computed result
     * @return a new ContinuableFuture that will complete with the result of the function
     * @throws Exception if the function throws an exception
     */
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

    //    public <U> ContinuableFuture<U> thenApply(final BiFunction<? super T, ? super Exception, U> action) {
    //        return new ContinuableFuture<U>(new Future<U>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone();
    //            }
    //
    //            @Override
    //            public U get() throws InterruptedException, ExecutionException {
    //                final Result<T, Exception> result = gett();
    //                return action.apply(result.orElse(null), result.getException());
    //            }
    //
    //            @Override
    //            public U get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                final Result<T, Exception> result = gett(timeout, unit);
    //                return action.apply(result.orElse(null), result.getException());
    //            }
    //        }, null, asyncExecutor) {
    //            @Override
    //            public boolean cancelAll(boolean mayInterruptIfRunning) {
    //                return super.cancelAll(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isAllCancelled() {
    //                return super.isAllCancelled();
    //            }
    //        };
    //    }

    //    public <U> ContinuableFuture<Void> thenAccept(final Consumer<? super T> action) {
    //        return thenApply(new Function<T, Void>() {
    //            @Override
    //            public Void apply(T t) {
    //                action.accept(t);
    //                return null;
    //            }
    //        });
    //    }
    //
    //    public <U> ContinuableFuture<Void> thenAccept(final BiConsumer<? super T, ? super Exception> action) {
    //        return thenApply(new BiFunction<T, ? super Exception, Void>() {
    //            @Override
    //            public Void apply(T t, ? super Exception e) {
    //                action.accept(t, e);
    //                return null;
    //            }
    //        });
    //    }
    //
    //    public <U, R> ContinuableFuture<R> thenCombine(final ContinuableFuture<U> other, final BiFunction<? super T, ? super U, ? extends R> action) {
    //        return new ContinuableFuture<R>(new Future<R>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning) && other.future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled() || other.future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone() && other.future.isDone();
    //            }
    //
    //            @Override
    //            public R get() throws InterruptedException, ExecutionException {
    //                return action.apply(future.get(), other.future.get());
    //            }
    //
    //            @Override
    //            public R get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                final long timeoutInMillis = unit.toMillis(timeout);
    //                final long now = N.currentMillis();
    //                final long endTime = timeoutInMillis > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeoutInMillis;
    //
    //                final T result = future.get(timeout, unit);
    //                final U otherResult = other.future.get(N.max(0, endTime - N.currentMillis()), TimeUnit.MILLISECONDS);
    //
    //                return action.apply(result, otherResult);
    //            }
    //        }, null, asyncExecutor) {
    //            @Override
    //            public boolean cancelAll(boolean mayInterruptIfRunning) {
    //                return super.cancelAll(mayInterruptIfRunning) && other.cancelAll(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isAllCancelled() {
    //                return super.isAllCancelled() && other.isAllCancelled();
    //            }
    //        };
    //    }
    //
    //    public <U, R> ContinuableFuture<R> thenCombine(final ContinuableFuture<U> other, final Function<? super Tuple4<T, ? super Exception, U, ? super Exception>, ? extends R> action) {
    //        return new ContinuableFuture<R>(new Future<R>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning) && other.future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled() || other.future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone() && other.future.isDone();
    //            }
    //
    //            @Override
    //            public R get() throws InterruptedException, ExecutionException {
    //                final Result<T, Exception> result = gett();
    //                final Pair<U, ? super Exception> result2 = other.gett();
    //
    //                return action.apply(Tuple.of(result.orElse(null), result.getException(), (U) result2.orElse(null), result2.getException()));
    //            }
    //
    //            @Override
    //            public R get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                final long timeoutInMillis = unit.toMillis(timeout);
    //                final long now = N.currentMillis();
    //                final long endTime = timeoutInMillis > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeoutInMillis;
    //
    //                final Result<T, Exception> result = ContinuableFuture.this.gett(timeout, unit);
    //                final Pair<U, ? super Exception> result2 = other.gett(N.max(0, endTime - N.currentMillis()), TimeUnit.MILLISECONDS);
    //
    //                return action.apply(Tuple.of(result.orElse(null), result.getException(), (U) result2.orElse(null), result2.getException()));
    //            }
    //        }, null, asyncExecutor) {
    //            @Override
    //            public boolean cancelAll(boolean mayInterruptIfRunning) {
    //                return super.cancelAll(mayInterruptIfRunning) && other.cancelAll(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isAllCancelled() {
    //                return super.isAllCancelled() && other.isAllCancelled();
    //            }
    //        };
    //    }
    //
    //    public <U> ContinuableFuture<Void> thenAcceptBoth(final ContinuableFuture<U> other, final BiConsumer<? super T, ? super U> action) {
    //        return thenCombine(other, new BiFunction<T, U, Void>() {
    //            @Override
    //            public Void apply(T t, U u) {
    //                action.accept(t, u);
    //                return null;
    //            }
    //        });
    //    }
    //
    //    public <U> ContinuableFuture<Void> thenAcceptBoth(final ContinuableFuture<U> other, final Consumer<? super Tuple4<T, ? super Exception, U, ? super Exception>> action) {
    //        return thenCombine(other, new Function<Tuple4<T, ? super Exception, U, ? super Exception>, Void>() {
    //            @Override
    //            public Void apply(Tuple4<T, ? super Exception, U, ? super Exception> t) {
    //                action.accept(t);
    //                return null;
    //            }
    //        });
    //    }

    /**
     * Executes the provided action when the computation of this ContinuableFuture is complete.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param action The action to be executed after the computation of this ContinuableFuture is complete.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            get();
            action.run();
            return null;
        });
    }

    /**
     * Executes the provided action when the computation of this ContinuableFuture is complete.
     * The action is a consumer that takes the result of the computation as input.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param action The action to be executed after the computation of this ContinuableFuture is complete. It's a consumer that takes the result of the computation.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get());
            return null;
        });
    }

    /**
     * Executes the provided action when the computation of this ContinuableFuture is complete.
     * The action is a bi-consumer that takes the result of the computation and any exception that occurred during the computation as input.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param action The action to be executed after the computation of this ContinuableFuture is complete. It's a bi-consumer that takes the result of the computation and any exception that occurred during the computation.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided Callable action when the computation of this ContinuableFuture is complete.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <R> The result type of the callable action, and of the returned ContinuableFuture
     * @param action The callable action to be executed after the computation of this ContinuableFuture is complete.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public <R> ContinuableFuture<R> thenCall(final Callable<R> action) {
        return execute(() -> {
            get();
            return action.call();
        });
    }

    /**
     * Executes the provided function when the computation of this ContinuableFuture is complete.
     * The function takes the result of the computation as input and returns a result.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param action The function to be executed after the computation of this ContinuableFuture is complete. It takes the result of the computation as input and returns a result.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get()));
    }

    /**
     * Executes the provided function when the computation of this ContinuableFuture is complete.
     * The function is a bi-function that takes the result of the computation and any exception that occurred during the computation as input, and returns a result.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param action The function to be executed after the computation of this ContinuableFuture is complete. It's a bi-function that takes the result of the computation and any exception that occurred during the computation, and returns a result.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
     * @see #gett()
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            return action.apply(result.orElseIfFailure(null), result.getException());
        });
    }

    /**
     * Executes the provided action after both this ContinuableFuture and the other ContinuableFuture complete.
     * If either of the ContinuableFutures is cancelled or fails, the action will not be executed.
     *
     * @param other The other ContinuableFuture that the action depends on.
     * @param action The action to be executed after both ContinuableFutures complete.
     * @return A new ContinuableFuture that represents the execution of the action.
     * @throws Exception if the action throws an exception.
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
     * Executes the provided action after both this ContinuableFuture and the other ContinuableFuture complete.
     * The action is a bi-consumer that takes the result of this ContinuableFuture and the result of the other ContinuableFuture as input.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param other The other ContinuableFuture that the action depends on.
     * @param action The action to be executed after both ContinuableFutures complete. It's a bi-consumer that takes the result of this ContinuableFuture and the result of the other ContinuableFuture.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public <U> ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiConsumer<? super T, ? super U, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get(), other.get());
            return null;
        }, other);
    }

    /**
     * Executes the provided action after both this ContinuableFuture and the other ContinuableFuture complete.
     * The action is a consumer that takes the result of this ContinuableFuture and the result of the other ContinuableFuture as input.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param other The other ContinuableFuture that the action depends on.
     * @param action The action to be executed after both ContinuableFutures complete. It's a consumer that takes the result of this ContinuableFuture and the result of the other ContinuableFuture.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided action after both this ContinuableFuture and the other ContinuableFuture complete.
     * The action takes the result of this ContinuableFuture, any exception that occurred during the computation of this ContinuableFuture,
     * the result of the other ContinuableFuture, and any exception that occurred during the computation of the other ContinuableFuture as input.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param other The other ContinuableFuture that the action depends on.
     * @param action The action to be executed after both ContinuableFutures complete. It's a consumer that takes the result of this ContinuableFuture and the result of the other ContinuableFuture.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided Callable action after both this ContinuableFuture and the other ContinuableFuture complete.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <R> The result type of the callable action, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the action depends on.
     * @param action The Callable action to be executed after the computation of this ContinuableFuture and the other ContinuableFuture is complete.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public <R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<?> other, final Callable<R> action) {
        return execute(() -> {
            get();
            other.get();
            return action.call();
        }, other);
    }

    /**
     * Executes the provided BiFunction after both this ContinuableFuture and the other ContinuableFuture complete.
     * The BiFunction takes the result of this ContinuableFuture and the result of the other ContinuableFuture as input, and returns a result.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the BiFunction.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param <R> The result type of the BiFunction, and of the returned ContinuableFuture.
     * @param other The other ContinuableFuture that the BiFunction depends on.
     * @param action The BiFunction to be executed after both ContinuableFutures complete. It takes the result of this ContinuableFuture and the result of the other ContinuableFuture, and returns a result.
     * @return A new ContinuableFuture representing pending completion of the BiFunction.
     * @throws Exception if the BiFunction throws an exception
     */
    public <U, R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get(), other.get()), other);
    }

    /**
     * Executes the provided BiFunction after both this ContinuableFuture and the other ContinuableFuture complete.
     * The BiFunction takes the result of this ContinuableFuture and the result of the other ContinuableFuture as input, and returns a result.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the BiFunction.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param <R> The result type of the BiFunction, and of the returned ContinuableFuture.
     * @param other The other ContinuableFuture that the BiFunction depends on.
     * @param action The BiFunction to be executed after both ContinuableFutures complete. It takes the result of this ContinuableFuture and the result of the other ContinuableFuture, and returns a result.
     * @return A new ContinuableFuture representing pending completion of the BiFunction.
     * @throws Exception if the BiFunction throws an exception
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
     * Executes the provided QuadFunction after both this ContinuableFuture and the other ContinuableFuture complete.
     * The QuadFunction takes the result of this ContinuableFuture, any exception that occurred during the computation of this ContinuableFuture,
     * the result of the other ContinuableFuture, and any exception that occurred during the computation of the other ContinuableFuture as input, and returns a result.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the QuadFunction.
     *
     * @param <U> The result type of the other ContinuableFuture.
     * @param <R> The result type of the QuadFunction, and of the returned ContinuableFuture.
     * @param other The other ContinuableFuture that the QuadFunction depends on.
     * @param action The QuadFunction to be executed after both ContinuableFutures complete. It takes the result of this ContinuableFuture, any exception that occurred during the computation of this ContinuableFuture,
     * the result of the other ContinuableFuture, and any exception that occurred during the computation of the other ContinuableFuture, and returns a result.
     * @return A new ContinuableFuture representing pending completion of the QuadFunction.
     * @throws Exception if the QuadFunction throws an exception
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
     * Executes the provided Runnable action after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The Runnable action to be executed after either ContinuableFuture completes.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided action after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The action to be executed after either ContinuableFuture completes. It's a consumer that takes the result of the first completed computation.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            final T result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            action.accept(result);
            return null;
        }, other);
    }

    /**
     * Executes the provided action after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The action to be executed after either ContinuableFuture completes. It's a consumer that takes the result of the first completed computation.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided Callable action after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <R> The result type of the callable action, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The Callable action to be executed after either ContinuableFuture completes.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<?> other, final Callable<? extends R> action) {
        return execute(() -> {
            Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            return action.call();
        }, other);
    }

    /**
     * Executes the provided function after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the function.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the function may depend on.
     * @param action The function to be executed after either ContinuableFuture completes. It takes the result of the first completed computation as input and returns a result.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final T result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            return action.apply(result);
        }, other);
    }

    /**
     * Executes the provided function after either this ContinuableFuture or the other ContinuableFuture completes.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the function.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the function may depend on.
     * @param action The function to be executed after either ContinuableFuture completes. It takes the result of the first completed computation as input and returns a result.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
     * @see #gett()
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).gett();

            return action.apply(result.orElseIfFailure(null), result.getException());
        }, other);
    }

    /**
     * Executes the provided Runnable action after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * If either of the ContinuableFutures fails, the action will not be executed.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The Runnable action to be executed after the first successful completion of either ContinuableFuture.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided action after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * If either of the ContinuableFutures fails, the action will not be executed.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The action to be executed after the first successful completion of either ContinuableFuture. It's a consumer that takes the result of the first successful computation.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided action after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * If either of the ContinuableFutures fails, the action will take the exception of first completed computation if both fails.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The action to be executed after the first successful completion of either ContinuableFuture. It's a consumer that takes the result of the first successful computation or the exception of first completed computation if both fails.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided Callable action after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the action.
     * If either of the ContinuableFutures fails, the action will not be executed.
     * This method does not block, it simply returns a new ContinuableFuture that will complete after executing the action.
     *
     * @param <R> The result type of the callable action, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the action may depend on.
     * @param action The Callable action to be executed after the first successful completion of either ContinuableFuture.
     * @return A new ContinuableFuture representing pending completion of the action.
     * @throws Exception if the action throws an exception
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
     * Executes the provided function after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the function.
     * If either of the ContinuableFutures fails, the function will not be executed.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the function may depend on.
     * @param action The function to be executed after the first successful completion of either ContinuableFuture. It takes the result of the first successful computation as input and returns a result.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
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
     * Executes the provided function after either this ContinuableFuture or the other ContinuableFuture completes successfully.
     * If both ContinuableFutures complete at the same time, there is no guarantee which ContinuableFuture's completion triggers the function.
     * If either of the ContinuableFutures fails, the action will take the exception of first completed computation if both fails.
     * This method does not block, it simply returns a new ContinuableFuture that will complete with the result of the function.
     *
     * @param <R> The result type of the function, and of the returned ContinuableFuture
     * @param other The other ContinuableFuture that the function may depend on.
     * @param action The function to be executed after the first successful completion of either ContinuableFuture. It takes the result of the first successful computation or the exception of first completed computation if both fails as input and returns a result or throw an exception.
     * @return A new ContinuableFuture representing pending completion of the function.
     * @throws Exception if the function throws an exception
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
    //     * Returns a new ContinuableFuture that, when either this or the
    //     * other given ContinuableFuture complete normally. If both of the given
    //     * ContinuableFutures complete exceptionally, then the returned
    //     * ContinuableFuture also does so.
    //     *
    //     * @param other
    //     * @param action
    //     * @return
    //     */
    //    public <U> ContinuableFuture<U> applyToEither(final ContinuableFuture<? extends T> other, final Function<? super T, U> action) {
    //        return Futures.anyOf(Array.asList(this, other)).thenApply(action);
    //    }
    //
    //    /**
    //     * Returns a new ContinuableFuture that, when either this or the
    //     * other given ContinuableFuture complete normally. If both of the given
    //     * ContinuableFutures complete exceptionally, then the returned
    //     * ContinuableFuture also does so.
    //     *
    //     * @param other
    //     * @param action
    //     * @return
    //     */
    //    public ContinuableFuture<Void> acceptEither(final ContinuableFuture<? extends T> other, final Consumer<? super T> action) {
    //        return Futures.anyOf(Array.asList(this, other)).thenAccept(action);
    //    }

    //    /**
    //     * Returns a new ContinuableFuture that, when this ContinuableFuture completes
    //     * exceptionally, is executed with this ContinuableFuture's exception as the
    //     * argument to the supplied function. Otherwise, if this ContinuableFuture
    //     * completes normally, then the returned ContinuableFuture also completes
    //     * normally with the same value.
    //     *
    //     * @param action
    //     * @return
    //     */
    //    public ContinuableFuture<T> exceptionally(final Function<Exception, ? extends T> action) {
    //        return new ContinuableFuture<T>(new Future<T>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone();
    //            }
    //
    //            @Override
    //            public T get() throws InterruptedException, ExecutionException {
    //                try {
    //                    return future.get();
    //                } catch (Exception e) {
    //                    return action.apply(e);
    //                }
    //            }
    //
    //            @Override
    //            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                try {
    //                    return future.get(timeout, unit);
    //                } catch (Exception e) {
    //                    return action.apply(e);
    //                }
    //            }
    //        }, null, asyncExecutor) {
    //            @Override
    //            public boolean cancelAll(boolean mayInterruptIfRunning) {
    //                return super.cancelAll(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isAllCancelled() {
    //                return super.isAllCancelled();
    //            }
    //        };
    //    }

    //    public ContinuableFuture<T> whenComplete(final BiConsumer<? super T, ? super Exception> action) {
    //        return new ContinuableFuture<>(new Future<T>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone();
    //            }
    //
    //            @Override
    //            public T get() throws InterruptedException, ExecutionException {
    //                final Result<T, Exception> result = gett();
    //
    //                if (result.right != null) {
    //                    try {
    //                        action.accept(result.orElse(null), result.getException());
    //                    } catch (Exception e) {
    //                        // ignore.
    //                    }
    //
    //                    if (result.right instanceof InterruptedException) {
    //                        throw ((InterruptedException) result.right);
    //                    } else if (result.right instanceof ExecutionException) {
    //                        throw ((ExecutionException) result.right);
    //                    } else {
    //                        throw ExceptionUtil.toRuntimeException(result.right);
    //                    }
    //                } else {
    //                    action.accept(result.orElse(null), result.getException());
    //                    return result.left;
    //                }
    //            }
    //
    //            @Override
    //            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                final Result<T, Exception> result = gett(timeout, unit);
    //
    //                if (result.right != null) {
    //                    try {
    //                        action.accept(result.orElse(null), result.getException());
    //                    } catch (Exception e) {
    //                        // ignore.
    //                    }
    //
    //                    if (result.right instanceof InterruptedException) {
    //                        throw ((InterruptedException) result.right);
    //                    } else if (result.right instanceof ExecutionException) {
    //                        throw ((ExecutionException) result.right);
    //                    } else {
    //                        throw ExceptionUtil.toRuntimeException(result.right);
    //                    }
    //                } else {
    //                    action.accept(result.orElse(null), result.getException());
    //                    return result.left;
    //                }
    //            }
    //        }, asyncExecutor);
    //    }
    //
    //    public <U> ContinuableFuture<U> handle(final BiFunction<? super T, ? super Exception, U> action) {
    //        return new ContinuableFuture<>(new Future<U>() {
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return future.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return future.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return future.isDone();
    //            }
    //
    //            @Override
    //            public U get() throws InterruptedException, ExecutionException {
    //                final Result<T, Exception> result = gett();
    //                return action.apply(result.orElse(null), result.getException());
    //            }
    //
    //            @Override
    //            public U get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                final Result<T, Exception> result = gett(timeout, unit);
    //                return action.apply(result.orElse(null), result.getException());
    //            }
    //        }, asyncExecutor);
    //    }

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
     * Configures this ContinuableFuture to delay the execution of the next action.
     * The delay is applied before the next action is executed.
     *
     * @param delay The delay before the next action is executed. If the delay is less than or equal to 0, this method has no effect.
     * @param unit The time unit of the delay parameter.
     * @return A new ContinuableFuture configured with the specified delay if the specified delay is bigger than 0, itself otherwise.
     */
    public ContinuableFuture<T> thenDelay(final long delay, final TimeUnit unit) {
        if (delay <= 0) {
            return this;
        }

        return with(asyncExecutor, delay, unit);
    }

    /**
     * Configures this ContinuableFuture to execute the next action using the specified executor.
     *
     * @param executor The executor to be used for the execution of the next action.
     * @return A new ContinuableFuture configured with the specified executor.
     */
    public ContinuableFuture<T> thenUse(final Executor executor) {
        return with(executor, 0, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param executor
     * @param delay
     * @param unit
     * @return
     * @deprecated
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
                    isDelayed = true;

                    N.sleepUninterruptibly(delayInMillis - (System.currentTimeMillis() - startTime));
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

    // Doesn't work.
    //    public CompletableFuture<T> toCompletableFuture() {
    //        return new CompletableFuture<T>() {
    //
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return ContinuableFuture.this.cancel(mayInterruptIfRunning);
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return ContinuableFuture.this.isAllCancelled();
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return ContinuableFuture.this.isDone();
    //            }
    //
    //            @Override
    //            public T get() throws InterruptedException, ExecutionException {
    //                return ContinuableFuture.this.get();
    //            }
    //
    //            @Override
    //            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                return ContinuableFuture.this.get(timeout, unit);
    //            }
    //        };
    //    }

    //    public CompletableFuture<T> toCompletableFuture() {
    //        return CompletableFuture.completedFuture(null).thenCompose(none -> {
    //            try {
    //                return CompletableFuture.completedFuture(this.get());
    //            } catch (InterruptedException e) {
    //                return CompletableFuture.failedFuture(e);
    //            } catch (ExecutionException e) {
    //                return CompletableFuture.failedFuture(e.getCause());
    //            }
    //        });
    //    }
}
