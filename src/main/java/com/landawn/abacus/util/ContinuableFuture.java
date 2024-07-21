/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Tuple.Tuple4;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ContinuableFuture.html">https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ContinuableFuture.html</a>
 * @since 0.8
 * @see Futures
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
     *
     * @param action
     * @return
     * @see N#asyncExecute(Throwables.Runnable)
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action) {
        return run(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     *
     * @param action
     * @param executor
     * @return
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
     *
     *
     * @param <T>
     * @param action
     * @return
     * @see N#asyncExecute(Callable)
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action) {
        return call(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     *
     *
     * @param <T>
     * @param action
     * @param executor
     * @return
     */
    public static <T> ContinuableFuture<T> call(final Callable<T> action, final Executor executor) {
        final FutureTask<T> futureTask = new FutureTask<>(action);

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     *
     * @param <T>
     * @param result
     * @return a ContinuableFuture which is already done by passing the result to it directly.
     */
    public static <T> ContinuableFuture<T> completed(final T result) {
        return new ContinuableFuture<>(new Future<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
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
     *
     * @param <T>
     * @param future
     * @return
     */
    public static <T> ContinuableFuture<T> wrap(Future<T> future) {
        return new ContinuableFuture<>(future);
    }

    /**
     *
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Checks if is cancelled.
     *
     * @return true, if is cancelled
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Cancel this future and all the previous stage future recursively.
     *
     * @param mayInterruptIfRunning
     * @return
     */
    public boolean cancelAll(boolean mayInterruptIfRunning) {
        boolean res = true;

        if (N.notEmpty(upFutures)) {
            for (ContinuableFuture<?> preFuture : upFutures) {
                res = res & preFuture.cancelAll(mayInterruptIfRunning); //NOSONAR
            }
        }

        return cancel(mayInterruptIfRunning) && res;
    }

    /**
     * Returns true if this future and all previous stage futures have been recursively cancelled, otherwise false is returned.
     *
     * @return true, if is all cancelled
     */
    public boolean isAllCancelled() {
        if (N.notEmpty(upFutures)) {
            for (ContinuableFuture<?> preFuture : upFutures) {
                if (preFuture.isAllCancelled()) {
                    return false;
                }
            }
        }

        return isCancelled();
    }

    /**
     * Checks if is done.
     *
     * @return true, if is done
     */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     *
     * @return
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     */
    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * Gets the t.
     *
     * @return
     */
    public Result<T, Exception> gett() {
        try {
            return Result.of(get(), null);
        } catch (Exception e) {
            return Result.of(null, e);
        }
    }

    /**
     * Gets the t.
     *
     * @param timeout
     * @param unit
     * @return
     */
    public Result<T, Exception> gett(final long timeout, final TimeUnit unit) {
        try {
            return Result.of(get(timeout, unit), null);
        } catch (Exception e) {
            return Result.of(null, e);
        }
    }

    /**
     * Gets the now.
     *
     * @param defaultValue
     * @return
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public T getNow(T defaultValue) throws InterruptedException, ExecutionException {
        if (isDone()) {
            return get();
        }

        return defaultValue;
    }

    /**
     * Gets the then apply.
     *
     * @param <U>
     * @param <E>
     * @param action
     * @return
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws E the e
     */
    public <U, E extends Exception> U getThenApply(final Throwables.Function<? super T, ? extends U, E> action)
            throws InterruptedException, ExecutionException, E {
        return action.apply(get());
    }

    /**
     * Gets the then apply.
     *
     * @param <U>
     * @param <E>
     * @param timeout
     * @param unit
     * @param action
     * @return
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     * @throws E the e
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit, final Throwables.Function<? super T, ? extends U, E> action)
            throws InterruptedException, ExecutionException, TimeoutException, E {
        return action.apply(get(timeout, unit));
    }

    /**
     * Gets the then apply.
     *
     * @param <U>
     * @param <E>
     * @param action
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U getThenApply(final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett();
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Gets the then apply.
     *
     * @param <U>
     * @param <E>
     * @param timeout
     * @param unit
     * @param action
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws E {
        final Result<T, Exception> result = gett(timeout, unit);
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Gets the then accept.
     *
     * @param <E>
     * @param action
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws E the e
     */
    public <E extends Exception> void getThenAccept(final Throwables.Consumer<? super T, E> action) throws InterruptedException, ExecutionException, E {
        action.accept(get());
    }

    /**
     * Gets the then accept.
     *
     * @param <E>
     * @param timeout
     * @param unit
     * @param action
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     * @throws TimeoutException the timeout exception
     * @throws E the e
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit, final Throwables.Consumer<? super T, E> action)
            throws InterruptedException, ExecutionException, TimeoutException, E {
        action.accept(get(timeout, unit));
    }

    /**
     * Gets the then accept.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void getThenAccept(final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws E {
        final Result<T, Exception> result = gett();
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Gets the then accept.
     *
     * @param <E>
     * @param timeout
     * @param unit
     * @param action
     * @throws E the e
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
    //            throw ExceptionUtil.toRuntimeException(e);
    //        }
    //    }
    //
    //    public void complete(BiConsumer<? super T, ? super Exception> action) {
    //        final Result<T, Exception> result = gett();
    //        action.accept(result.orElse(null), result.getException());
    //    }

    /**
     *
     * @param <U>
     * @param func
     * @return
     */
    public <U> ContinuableFuture<U> map(final Throwables.Function<? super T, ? extends U, ? extends Exception> func) {
        return new ContinuableFuture<>(new Future<U>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
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
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            @Override
            public U get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final T ret = ContinuableFuture.this.get(timeout, unit);

                try {
                    return func.apply(ret);
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }, null, asyncExecutor) {
            @Override
            public boolean cancelAll(boolean mayInterruptIfRunning) {
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
     *
     * @param action
     * @return
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            get();
            action.run();
            return null;
        });
    }

    /**
     *
     * @param action
     * @return
     */
    public ContinuableFuture<Void> thenRun(final Throwables.Consumer<? super T, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get());
            return null;
        });
    }

    /**
     *
     * @param action
     * @return
     */
    public ContinuableFuture<Void> thenRun(final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        });
    }

    /**
     *
     *
     * @param <R>
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> thenCall(final Callable<R> action) {
        return execute(() -> {
            get();
            return action.call();
        });
    }

    /**
     *
     * @param <R>
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get()));
    }

    /**
     *
     * @param <R>
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> thenCall(final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = gett();
            return action.apply(result.orElseIfFailure(null), result.getException());
        });
    }

    /**
     * Run after both.
     *
     * @param other
     * @param action
     * @return
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
     * Run after both.
     *
     * @param <U>
     * @param other
     * @param action
     * @return
     */
    public <U> ContinuableFuture<Void> runAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiConsumer<? super T, ? super U, ? extends Exception> action) {
        return execute(() -> {
            action.accept(get(), other.get());
            return null;
        }, other);
    }

    /**
     * Run after both.
     *
     * @param <U>
     * @param other
     * @param action
     * @return
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
     * Run after both.
     *
     * @param <U>
     * @param other
     * @param action
     * @return
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
     * Call after both.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<?> other, final Callable<R> action) {
        return execute(() -> {
            get();
            other.get();
            return action.call();
        }, other);
    }

    /**
     * Call after both.
     *
     * @param <U>
     * @param <R>
     * @param other
     * @param action
     * @return
     */
    public <U, R> ContinuableFuture<R> callAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends Exception> action) {
        return execute(() -> action.apply(get(), other.get()), other);
    }

    /**
     * Call after both.
     *
     * @param <U>
     * @param <R>
     * @param other
     * @param action
     * @return
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
     * Call after both.
     *
     * @param <U>
     * @param <R>
     * @param other
     * @param action
     * @return
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
     * Run after either.
     *
     * @param other
     * @param action
     * @return
     */
    public ContinuableFuture<Void> runAfterEither(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action) {
        return execute(() -> {
            Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            action.run();
            return null;
        }, other);
    }

    /**
     * Run after either.
     *
     * @param other
     * @param action
     * @return
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
     * Run after either.
     *
     * @param other
     * @param action
     * @return
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
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<?> other, final Callable<? extends R> action) {
        return execute(() -> {
            Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            return action.call();
        }, other);
    }

    /**
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final T result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).get();

            return action.apply(result);
        }, other);
    }

    /**
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
     */
    public <R> ContinuableFuture<R> callAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action) {
        return execute(() -> {
            final Result<T, Exception> result = Futures.anyOf(Array.asList(ContinuableFuture.this, other)).gett();

            return action.apply(result.orElseIfFailure(null), result.getException());
        }, other);
    }

    /**
     * Run after either.
     *
     * @param other
     * @param action
     * @return
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
     * Run after either.
     *
     * @param other
     * @param action
     * @return
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
     * Run after either.
     *
     * @param other
     * @param action
     * @return
     */
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
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
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
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
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
     * Call after either.
     *
     * @param <R>
     * @param other
     * @param action
     * @return
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

    /**
     *
     * @param <R>
     * @param command
     * @return
     */
    private <R> ContinuableFuture<R> execute(final Callable<R> command) {
        return execute(command, null);
    }

    /**
     *
     * @param <R>
     * @param command
     * @param other
     * @return
     */
    private <R> ContinuableFuture<R> execute(final Callable<R> command, final ContinuableFuture<?> other) {
        return execute(new FutureTask<>(command), other);
    }

    /**
     *
     * @param <U>
     * @param futureTask
     * @param other
     * @return
     */
    private <U> ContinuableFuture<U> execute(final FutureTask<U> futureTask, final ContinuableFuture<?> other) {
        asyncExecutor.execute(futureTask);

        @SuppressWarnings("rawtypes")
        final List<ContinuableFuture<?>> upFutureList = other == null ? (List) Arrays.asList(this) : Arrays.asList(this, other);
        return new ContinuableFuture<>(futureTask, upFutureList, asyncExecutor);
    }

    /**
     *
     * @param delay
     * @param unit
     * @return
     */
    public ContinuableFuture<T> thenDelay(long delay, TimeUnit unit) {
        if (delay <= 0) {
            return this;
        }

        return with(asyncExecutor, delay, unit);
    }

    /**
     *
     * @param executor
     * @return
     */
    public ContinuableFuture<T> thenUse(Executor executor) {
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

        return new ContinuableFuture<>(new Future<T>() {
            private final long delayInMillis = unit.toMillis(delay);
            private final long startTime = System.currentTimeMillis();
            private volatile boolean isDelayed = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                boolean isDone = future.isDone();

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
            public boolean cancelAll(boolean mayInterruptIfRunning) { //NOSONAR
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
