/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

/**
 * The Futures class provides utility methods for working with Future objects.
 * These methods include combining multiple Future objects, creating a Future that completes when all input Futures complete,
 * creating a Future that completes when any input Future completes, and iterating over a collection of Futures.
 *
 * @see ContinuableFuture
 * @see CompletableFuture
 * @see ExecutorCompletionService
 */
public final class Futures {

    private Futures() throws IllegalArgumentException {
        // singleton.
    }

    // Doesn't work.
    //    public static <T> CompletableFuture<T> toCompletableFuture(final Future<? extends T> f) {
    //        N.checkArgNotNull(f, "future");
    //
    //        return new CompletableFuture<>() {
    //            @Override
    //            public T get() throws InterruptedException, ExecutionException {
    //                return f.get();
    //
    //            }
    //
    //            @Override
    //            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    //                return f.get(timeout, unit);
    //            }
    //
    //            @Override
    //            public boolean isDone() {
    //                return f.isDone();
    //            }
    //
    //            @Override
    //            public boolean isCancelled() {
    //                return f.isCancelled();
    //            }
    //
    //            @Override
    //            public boolean cancel(boolean mayInterruptIfRunning) {
    //                return f.cancel(mayInterruptIfRunning);
    //            }
    //        };
    //    }

    /**
     * Composes two futures into a new ContinuableFuture that completes when both input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the two input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param zipFunctionForGet The function to be applied to the results of the two input futures.
     * @return A new ContinuableFuture that completes when both input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2));
    }

    /**
     * Composes two futures into a new ContinuableFuture that completes when both input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the two input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @return A new ContinuableFuture that completes when both input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple4<Future<T1>, Future<T2>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2);

        return compose(cfs, c -> zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1)),
                t -> zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), t._2, t._3)));
    }

    /**
     * Composes three futures into a new ContinuableFuture that completes when all input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the three input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @return A new ContinuableFuture that completes when all input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, cf3, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2, t._3));
    }

    /**
     * Composes three futures into a new ContinuableFuture that completes when all input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the three input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @return A new ContinuableFuture that completes when all input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2, cf3);

        return compose(cfs, c -> zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1), (Future<T3>) c.get(2)),
                t -> zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), (Future<T3>) t._1.get(2), t._2, t._3)));
    }

    /**
     * Composes multiple futures into a new ContinuableFuture that completes when all input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the input futures.
     *
     * @param <T> The result type of the input futures.
     * @param <FC> The collection type of the input futures.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cfs The collection of input futures.
     * @return A new ContinuableFuture that completes when all input futures complete, with a result computed by the provided function.
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet) {
        return compose(cfs, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1));
    }

    /**
     * Composes multiple futures into a new ContinuableFuture that completes when all input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the input futures.
     *
     * @param <T> The result type of the input futures.
     * @param <FC> The collection type of the input futures.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cfs The collection of input futures.
     * @return A new ContinuableFuture that completes when all input futures complete, with a result computed by the provided function.
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple3<FC, Long, TimeUnit>, ? extends R, Exception> zipFunctionTimeoutGet) throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty"); //NOSONAR
        N.checkArgNotNull(zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet);

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R get() throws InterruptedException, ExecutionException {
                try {
                    return zipFunctionForGet.apply(cfs);
                } catch (InterruptedException | ExecutionException e) {
                    throw e;
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public R get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Tuple3<FC, Long, TimeUnit> t = Tuple.of(cfs, timeout, unit);

                try {
                    return zipFunctionTimeoutGet.apply(t);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw e;
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        });
    }

    /**
     * Combines two futures into a new ContinuableFuture that completes when both input futures complete.
     * The result of the new ContinuableFuture is a Tuple2 containing the results of the two input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @return A new ContinuableFuture that completes when both input futures complete, with a result being a Tuple2 of the results of the two input futures.
     */
    public static <T1, T2> ContinuableFuture<Tuple2<T1, T2>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2) {
        return allOf(Arrays.asList(cf1, cf2)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures into a new ContinuableFuture that completes when all three input futures complete.
     * The result of the new ContinuableFuture is a Tuple3 containing the results of the three input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @return A new ContinuableFuture that completes when all three input futures complete, with a result being a Tuple3 of the results of the three input futures.
     */
    public static <T1, T2, T3> ContinuableFuture<Tuple3<T1, T2, T3>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2)));
    }

    /**
     * Combines four futures into a new ContinuableFuture that completes when all four input futures complete.
     * The result of the new ContinuableFuture is a Tuple4 containing the results of the four input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <T4> The result type of the fourth input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @param cf4 The fourth input future.
     * @return A new ContinuableFuture that completes when all four input futures complete, with a result being a Tuple4 of the results of the four input futures.
     */
    public static <T1, T2, T3, T4> ContinuableFuture<Tuple4<T1, T2, T3, T4>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3, final Future<? extends T4> cf4) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3)));
    }

    /**
     * Combines five futures into a new ContinuableFuture that completes when all five input futures complete.
     * The result of the new ContinuableFuture is a Tuple5 containing the results of the five input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <T4> The result type of the fourth input future.
     * @param <T5> The result type of the fifth input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @param cf4 The fourth input future.
     * @param cf5 The fifth input future.
     * @return A new ContinuableFuture that completes when all five input futures complete, with a result being a Tuple5 of the results of the five input futures.
     */
    public static <T1, T2, T3, T4, T5> ContinuableFuture<Tuple5<T1, T2, T3, T4, T5>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4)));
    }

    /**
     * Combines six futures into a new ContinuableFuture that completes when all six input futures complete.
     * The result of the new ContinuableFuture is a Tuple6 containing the results of the six input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <T4> The result type of the fourth input future.
     * @param <T5> The result type of the fifth input future.
     * @param <T6> The result type of the sixth input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @param cf4 The fourth input future.
     * @param cf5 The fifth input future.
     * @param cf6 The sixth input future.
     * @return A new ContinuableFuture that completes when all six input futures complete, with a result being a Tuple6 of the results of the six input futures.
     */
    public static <T1, T2, T3, T4, T5, T6> ContinuableFuture<Tuple6<T1, T2, T3, T4, T5, T6>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5,
            final Future<? extends T6> cf6) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6))
                .map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5)));
    }

    /**
     * Combines seven futures into a new ContinuableFuture that completes when all seven input futures complete.
     * The result of the new ContinuableFuture is a Tuple7 containing the results of the seven input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <T4> The result type of the fourth input future.
     * @param <T5> The result type of the fifth input future.
     * @param <T6> The result type of the sixth input future.
     * @param <T7> The result type of the seventh input future.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @param cf4 The fourth input future.
     * @param cf5 The fifth input future.
     * @param cf6 The sixth input future.
     * @param cf7 The seventh input future.
     * @return A new ContinuableFuture that completes when all seven input futures complete, with a result being a Tuple7 of the results of the seven input futures.
     */
    public static <T1, T2, T3, T4, T5, T6, T7> ContinuableFuture<Tuple7<T1, T2, T3, T4, T5, T6, T7>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5,
            final Future<? extends T6> cf6, final Future<? extends T7> cf7) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6, cf7))
                .map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5), (T7) t.get(6)));
    }

    /**
     * Combines two futures into a new ContinuableFuture that completes when both input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the two input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @return A new ContinuableFuture that completes when both input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Throwables.BiFunction<? super T1, ? super T2, ? extends R, ? extends Exception> action) {
        return allOf(Arrays.asList(cf1, cf2)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures into a new ContinuableFuture that completes when all three input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the three input futures.
     *
     * @param <T1> The result type of the first input future.
     * @param <T2> The result type of the second input future.
     * @param <T3> The result type of the third input future.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cf1 The first input future.
     * @param cf2 The second input future.
     * @param cf3 The third input future.
     * @return A new ContinuableFuture that completes when all three input futures complete, with a result computed by the provided function.
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2, final Future<? extends T3> cf3,
            final Throwables.TriFunction<? super T1, ? super T2, ? super T3, ? extends R, ? extends Exception> action) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1), (T3) t.get(2)));
    }

    /**
     * Combines multiple futures into a new ContinuableFuture that completes when all input futures complete.
     * The result of the new ContinuableFuture is determined by applying the provided function to the results of the input futures.
     *
     * @param <T> The result type of the input futures.
     * @param <R> The result type of the new ContinuableFuture.
     * @param cfs The collection of input futures.
     * @return A new ContinuableFuture that completes when all input futures complete, with a result computed by the provided function.
     */
    public static <T, R> ContinuableFuture<R> combine(final Collection<? extends Future<? extends T>> cfs,
            final Throwables.Function<List<T>, ? extends R, ? extends Exception> action) {
        final ContinuableFuture<List<T>> f = allOf(cfs);
        return f.map(action);
    }

    //    public static <T, R> Future<R> combine(final List<? extends Future<? extends T>> cfs, final Try.Function<List<T>, ? extends R, ? extends Exception> action) {
    //        final Future<List<T>> future = allOf(cfs);
    //        return future.thenApply(action);
    //    }

    /**
     * Returns a new ContinuableFuture that is completed when all the given Futures complete.
     * If any of the given Futures complete exceptionally, then the returned ContinuableFuture also does so.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The array of input futures.
     * @return A new ContinuableFuture that completes when all input futures complete.
     */
    @SafeVarargs
    public static <T> ContinuableFuture<List<T>> allOf(final Future<? extends T>... cfs) {
        return allOf2(Arrays.asList(cfs));
    }

    /**
     * Returns a new ContinuableFuture that is completed when all the given Futures complete.
     * If any of the given Futures complete exceptionally, then the returned ContinuableFuture also does so.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The collection of input futures.
     * @return A new ContinuableFuture that completes when all input futures complete.
     */
    public static <T> ContinuableFuture<List<T>> allOf(final Collection<? extends Future<? extends T>> cfs) {
        return allOf2(cfs);
    }

    private static <T> ContinuableFuture<List<T>> allOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public List<T> get() throws InterruptedException, ExecutionException {
                final List<T> result = new ArrayList<>(cfs.size());

                for (final Future<? extends T> future : cfs) {
                    result.add(future.get());
                }

                return result;
            }

            @Override
            public List<T> get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final long timeoutInMillis = unit.toMillis(timeout);
                final long now = System.currentTimeMillis();
                final long endTime = timeoutInMillis > Long.MAX_VALUE - now ? Long.MAX_VALUE : now + timeoutInMillis;

                final List<T> result = new ArrayList<>(cfs.size());

                for (final Future<? extends T> future : cfs) {
                    result.add(future.get(N.max(0, endTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS));
                }

                return result;
            }
        });
    }

    /**
     * Returns a new ContinuableFuture that is completed when any of the given Futures complete.
     * If all the given Futures complete exceptionally, then the returned ContinuableFuture also does so.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The array of input futures.
     * @return A new ContinuableFuture that completes when any input future completes.
     */
    @SafeVarargs
    public static <T> ContinuableFuture<T> anyOf(final Future<? extends T>... cfs) {
        return anyOf2(Arrays.asList(cfs));
    }

    /**
     * Returns a new ContinuableFuture that is completed when any of the given Futures complete.
     * If all the given Futures complete exceptionally, then the returned ContinuableFuture also does so.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The collection of input futures.
     * @return A new ContinuableFuture that completes when any input future completes.
     */
    public static <T> ContinuableFuture<T> anyOf(final Collection<? extends Future<? extends T>> cfs) {
        return anyOf2(cfs);
    }

    /**
     * Returns a {@link ContinuableFuture} that is completed when any of the provided futures in the collection are completed.
     * The returned future allows managing the lifecycle (e.g., cancellation) of the combined set of futures and propagates the result
     * or exception of the first completed future.
     *
     * @param <T> the type of the result produced by the futures
     * @param cfs the collection of futures to monitor for completion; cannot be null or empty
     * @return a continuable future that completes when any of the provided futures completes
     * @throws IllegalArgumentException if the provided collection of futures is null or empty
     */
    private static <T> ContinuableFuture<T> anyOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (!future.isCancelled()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (future.isDone()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                final Iterator<Result<T, Exception>> iter = iterate(cfs, Fn.identity());
                Result<T, Exception> result = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.isSuccess()) {
                        return result.orElseIfFailure(null);
                    }
                }

                //noinspection DataFlowIssue
                if (result.isFailure()) {
                    if (result.getException() instanceof InterruptedException) {
                        throw ((InterruptedException) result.getException());
                    } else if (result.getException() instanceof ExecutionException) {
                        throw ((ExecutionException) result.getException());
                    } else {
                        throw ExceptionUtil.toRuntimeException(result.getException(), true);
                    }
                }

                return result.orElseIfFailure(null);
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Iterator<Result<T, Exception>> iter = iterate(cfs, timeout, unit, Fn.identity());
                Result<T, Exception> result = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.isSuccess()) {
                        return result.orElseIfFailure(null);
                    }
                }

                //noinspection DataFlowIssue
                return handle(result);
            }
        });
    }

    /**
     * Returns an {@code Iterator} with elements got from the specified {@code futures}, first finished future, first out.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The array of input futures.
     * @return An {@code Iterator} that provides the results of the input futures as they complete.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final Future<? extends T>... cfs) {
        return iterate02(Arrays.asList(cfs));
    }

    /**
     * Returns an {@code Iterator} with elements got from the specified {@code futures}, first finished future, first out.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The collection of input futures.
     * @return An {@code Iterator} that provides the results of the input futures as they complete.
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs);
    }

    /**
     * Returns an {@code Iterator} with elements got from the specified {@code futures}, first finished future, first out.
     *
     * @param <T> The result type of the input futures.
     * @param cfs The collection of input futures.
     * @param totalTimeoutForAll The total timeout for all futures.
     * @param unit The time unit of the total timeout.
     * @return An {@code Iterator} that provides the results of the input futures as they complete.
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        return iterate02(cfs, totalTimeoutForAll, unit);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        final Iterator<Result<T, Exception>> iter = iterate02(cfs, totalTimeoutForAll, unit, Fn.identity());

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final Result<T, Exception> result = iter.next();

                return result.orElseThrow(Fn.toRuntimeException());
            }
        };
    }

    /**
     * Returns an {@code Iterator} with elements obtained from the specified {@code futures}, with the first finished future being the first out.
     * The elements are processed using the provided {@code resultHandler} function.
     *
     * @param <T> The result type of the input futures.
     * @param <R> The result type of the output after applying the resultHandler function.
     * @param cfs The collection of input futures.
     * @param resultHandler The function to process the results of the input futures.
     * @return An {@code Iterator} that provides the results of the input futures as they complete, processed by the resultHandler function.
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, resultHandler);
    }

    /**
     * Returns an {@code Iterator} with elements obtained from the specified {@code futures}, with the first finished future being the first out.
     * The elements are processed using the provided {@code resultHandler} function.
     *
     * @param <T> The result type of the input futures.
     * @param <R> The result type of the output after applying the resultHandler function.
     * @param cfs The collection of input futures.
     * @param totalTimeoutForAll The total timeout for all futures.
     * @param unit The time unit of the total timeout.
     * @param resultHandler The function to process the results of the input futures.
     * @return An {@code Iterator} that provides the results of the input futures as they complete, processed by the resultHandler function.
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, totalTimeoutForAll, unit, resultHandler);
    }

    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS, resultHandler);
    }

    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        N.checkArgPositive(totalTimeoutForAll, cs.totalTimeoutForAll);
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(resultHandler, cs.resultHandler);

        final long now = System.currentTimeMillis();
        final long totalTimeoutForAllInMillis = totalTimeoutForAll == Long.MAX_VALUE ? Long.MAX_VALUE : unit.toMillis(totalTimeoutForAll);

        return new ObjIterator<>() {
            private final Set<Future<? extends T>> activeFutures = N.newSetFromMap(new IdentityHashMap<>());

            { //NOSONAR
                activeFutures.addAll(cfs);
            }

            @Override
            public boolean hasNext() {
                return activeFutures.size() > 0;
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                while (true) {
                    for (final Future<? extends T> cf : activeFutures) {
                        if (cf.isDone()) {
                            try {
                                return resultHandler.apply(Result.of(cf.get(), null));
                            } catch (final Exception e) {
                                return resultHandler.apply(Result.of(null, e));
                            } finally {
                                activeFutures.remove(cf);
                            }
                        }
                    }

                    if (System.currentTimeMillis() - now >= totalTimeoutForAllInMillis) {
                        return resultHandler.apply(Result.of(null, new TimeoutException()));
                    }

                    N.sleepUninterruptibly(1);
                }
            }
        };
    }

    private static <R> R handle(final Result<R, Exception> result) throws InterruptedException, ExecutionException, TimeoutException {
        if (result.isFailure()) {
            if (result.getException() instanceof InterruptedException) {
                throw ((InterruptedException) result.getException());
            } else if (result.getException() instanceof ExecutionException) {
                throw ((ExecutionException) result.getException());
            } else if (result.getException() instanceof TimeoutException) {
                throw ((TimeoutException) result.getException());
            } else {
                throw ExceptionUtil.toRuntimeException(result.getException(), true);
            }
        }

        return result.orElseIfFailure(null);
    }
}
