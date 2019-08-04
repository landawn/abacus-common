/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.landawn.abacus.util.Try.TriFunction;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

/**
 * @since 0.9
 * 
 * @author Haiyang Li 
 *
 */
public final class Futures {
    private Futures() {
        // singleton.
    }

    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Try.BiFunction<? super Future<T1>, ? super Future<T2>, R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, zipFunctionForGet, new Try.Function<Tuple4<Future<T1>, Future<T2>, Long, TimeUnit>, R, Exception>() {
            @Override
            public R apply(Tuple4<Future<T1>, Future<T2>, Long, TimeUnit> t) throws Exception {
                return zipFunctionForGet.apply(t._1, t._2);
            }
        });
    }

    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Try.BiFunction<? super Future<T1>, ? super Future<T2>, R, Exception> zipFunctionForGet,
            final Try.Function<? super Tuple4<Future<T1>, Future<T2>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2);

        return compose(cfs, new Try.Function<List<Future<?>>, R, Exception>() {
            @Override
            public R apply(List<Future<?>> c) throws Exception {
                return zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1));
            }
        }, new Try.Function<Tuple3<List<Future<?>>, Long, TimeUnit>, R, Exception>() {
            @Override
            public R apply(Tuple3<List<Future<?>>, Long, TimeUnit> t) throws Exception {
                return zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), t._2, t._3));
            }
        });
    }

    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Try.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, cf3, zipFunctionForGet, new Try.Function<Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit>, R, Exception>() {
            @Override
            public R apply(Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit> t) throws Exception {
                return zipFunctionForGet.apply(t._1, t._2, t._3);
            }
        });
    }

    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Try.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, R, Exception> zipFunctionForGet,
            final Try.Function<? super Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2, cf3);

        return compose(cfs, new Try.Function<List<Future<?>>, R, Exception>() {
            @Override
            public R apply(List<Future<?>> c) throws Exception {
                return zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1), (Future<T3>) c.get(2));
            }
        }, new Try.Function<Tuple3<List<Future<?>>, Long, TimeUnit>, R, Exception>() {
            @Override
            public R apply(Tuple3<List<Future<?>>, Long, TimeUnit> t) throws Exception {
                return zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), (Future<T3>) t._1.get(2), t._2, t._3));
            }
        });
    }

    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Try.Function<? super FC, R, Exception> zipFunctionForGet) {
        return compose(cfs, zipFunctionForGet, new Try.Function<Tuple3<FC, Long, TimeUnit>, R, Exception>() {
            @Override
            public R apply(Tuple3<FC, Long, TimeUnit> t) throws Exception {
                return zipFunctionForGet.apply(t._1);
            }
        });
    }

    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Try.Function<? super FC, R, Exception> zipFunctionForGet,
            final Try.Function<? super Tuple3<FC, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        N.checkArgument(N.notNullOrEmpty(cfs), "'cfs' can't be null or empty");
        N.checkArgNotNull(zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet);

        return ContinuableFuture.wrap(new Future<R>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);
                    } catch (RuntimeException e) {
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
                for (Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (Future<?> future : cfs) {
                    if (future.isDone() == false) {
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
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }

            @Override
            public R get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Tuple3<FC, Long, TimeUnit> t = Tuple.of(cfs, timeout, unit);

                try {
                    return zipFunctionTimeoutGet.apply(t);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw e;
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        });
    }

    public static <T1, T2, E extends Exception> ContinuableFuture<Tuple2<T1, T2>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2) {
        return allOf(Arrays.asList(cf1, cf2)).map(new Try.Function<List<Object>, Tuple2<T1, T2>, E>() {
            @Override
            public Tuple2<T1, T2> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1));
            }
        });
    }

    public static <T1, T2, T3, E extends Exception> ContinuableFuture<Tuple3<T1, T2, T3>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(new Try.Function<List<Object>, Tuple3<T1, T2, T3>, E>() {
            @Override
            public Tuple3<T1, T2, T3> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2));
            }
        });
    }

    public static <T1, T2, T3, T4, E extends Exception> ContinuableFuture<Tuple4<T1, T2, T3, T4>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4)).map(new Try.Function<List<Object>, Tuple4<T1, T2, T3, T4>, E>() {
            @Override
            public Tuple4<T1, T2, T3, T4> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3));
            }
        });
    }

    public static <T1, T2, T3, T4, T5, E extends Exception> ContinuableFuture<Tuple5<T1, T2, T3, T4, T5>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5)).map(new Try.Function<List<Object>, Tuple5<T1, T2, T3, T4, T5>, E>() {
            @Override
            public Tuple5<T1, T2, T3, T4, T5> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4));
            }
        });
    }

    public static <T1, T2, T3, T4, T5, T6, E extends Exception> ContinuableFuture<Tuple6<T1, T2, T3, T4, T5, T6>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5,
            final Future<? extends T6> cf6) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6)).map(new Try.Function<List<Object>, Tuple6<T1, T2, T3, T4, T5, T6>, E>() {
            @Override
            public Tuple6<T1, T2, T3, T4, T5, T6> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5));
            }
        });
    }

    public static <T1, T2, T3, T4, T5, T6, T7, E extends Exception> ContinuableFuture<Tuple7<T1, T2, T3, T4, T5, T6, T7>> combine(
            final Future<? extends T1> cf1, final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4,
            final Future<? extends T5> cf5, final Future<? extends T6> cf6, final Future<? extends T7> cf7) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6, cf7)).map(new Try.Function<List<Object>, Tuple7<T1, T2, T3, T4, T5, T6, T7>, E>() {
            @Override
            public Tuple7<T1, T2, T3, T4, T5, T6, T7> apply(List<Object> t) {
                return Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5), (T7) t.get(6));
            }
        });
    }

    public static <T1, T2, R, E extends Exception> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Try.BiFunction<? super T1, ? super T2, R, E> action) {
        return allOf(Arrays.asList(cf1, cf2)).map(new Try.Function<List<Object>, R, E>() {
            @Override
            public R apply(List<Object> t) throws E {
                return action.apply((T1) t.get(0), (T2) t.get(1));
            }
        });
    }

    public static <T1, T2, T3, R, E extends Exception> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3, final TriFunction<? super T1, ? super T2, ? super T3, R, E> action) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(new Try.Function<List<Object>, R, E>() {
            @Override
            public R apply(List<Object> t) throws E {
                return action.apply((T1) t.get(0), (T2) t.get(1), (T3) t.get(2));
            }
        });
    }

    public static <T, R, E extends Exception> ContinuableFuture<R> combine(final Collection<? extends Future<? extends T>> cfs,
            final Try.Function<List<T>, R, E> action) {
        final ContinuableFuture<List<T>> f = allOf(cfs);
        return f.map(action);
    }

    //    public static <T, R, E extends Exception> Future<R> combine(final List<? extends Future<? extends T>> cfs, final Try.Function<List<T>, R, E> action) {
    //        final Future<List<T>> future = allOf(cfs);
    //        return future.thenApply(action);
    //    }

    /**
     * Returns a new Future that is completed when all of
     * the given Futures complete. If any of the given
     * Futures complete exceptionally, then the returned
     * Future also does so.
     * 
     * @param cfs
     * @return
     */
    @SafeVarargs
    public static <T> ContinuableFuture<List<T>> allOf(final Future<? extends T>... cfs) {
        return allOf2(Arrays.asList(cfs));
    }

    /**
     * Returns a new Future that is completed when all of
     * the given Futures complete. If any of the given
     * Futures complete exceptionally, then the returned
     * Future also does so.
     * 
     * @param cfs
     * @return
     */
    public static <T> ContinuableFuture<List<T>> allOf(final Collection<? extends Future<? extends T>> cfs) {
        return allOf2(cfs);
    }

    private static <T> ContinuableFuture<List<T>> allOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notNullOrEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<List<T>>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);
                    } catch (RuntimeException e) {
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
                for (Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (Future<?> future : cfs) {
                    if (future.isDone() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public List<T> get() throws InterruptedException, ExecutionException {
                final List<T> result = new ArrayList<>(cfs.size());

                for (Future<? extends T> future : cfs) {
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

                for (Future<? extends T> future : cfs) {
                    result.add(future.get(N.max(0, endTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS));
                }

                return result;
            }
        });
    }

    /**
     * Returns a new Future that, when any of the given Futures complete normally. 
     * If all of the given Futures complete exceptionally, then the returned Future also does so.
     * 
     * @param cfs
     * @return
     */
    @SafeVarargs
    public static <T> ContinuableFuture<T> anyOf(final Future<? extends T>... cfs) {
        return anyOf2(Arrays.asList(cfs));
    }

    /**
     * Returns a new Future that, when any of the given Futures complete normally. 
     * If all of the given Futures complete exceptionally, then the returned Future also does so.
     * 
     * @param cfs
     * @return
     */
    public static <T> ContinuableFuture<T> anyOf(final Collection<? extends Future<? extends T>> cfs) {
        return anyOf2(cfs);
    }

    private static <T> ContinuableFuture<T> anyOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notNullOrEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);
                    } catch (RuntimeException e) {
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
                for (Future<?> future : cfs) {
                    if (future.isCancelled() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public boolean isDone() {
                for (Future<?> future : cfs) {
                    if (future.isDone()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                final Iterator<Pair<T, Exception>> iter = iteratte(cfs);
                Pair<T, Exception> result = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.right == null) {
                        return result.left;
                    }
                }

                return handle(result);
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Iterator<Pair<T, Exception>> iter = iteratte(cfs, timeout, unit);
                Pair<T, Exception> result = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.right == null) {
                        return result.left;
                    }
                }

                return handle(result);
            }
        });
    }

    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final Future<? extends T>... cfs) {
        return iterate02(Arrays.asList(cfs));
    }

    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs);
    }

    /**
     * 
     * @param cfs 
     * @param totalTimeoutForAll
     * @param unit
     * @return
     * @see {@code ExecutorCompletionService}
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        return iterate02(cfs, totalTimeoutForAll, unit);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        return new ObjIterator<T>() {
            private final Iterator<Pair<T, Exception>> iter = iterate22(cfs, totalTimeoutForAll, unit);

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final Pair<T, Exception> result = iter.next();

                if (result.right != null) {
                    throw N.toRuntimeException(result.right);
                }

                return result.left;
            }
        };
    }

    @SafeVarargs
    public static <T> ObjIterator<Pair<T, Exception>> iteratte(final Future<? extends T>... cfs) {
        return iterate22(Arrays.asList(cfs));
    }

    public static <T> ObjIterator<Pair<T, Exception>> iteratte(final Collection<? extends Future<? extends T>> cfs) {
        return iterate22(cfs);
    }

    /**
     * 
     * @param cfs 
     * @param totalTimeoutForAll
     * @param unit
     * @return
     * @see {@code ExecutorCompletionService}
     */
    public static <T> ObjIterator<Pair<T, Exception>> iteratte(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll,
            final TimeUnit unit) {
        return iterate22(cfs, totalTimeoutForAll, unit);
    }

    private static <T> ObjIterator<Pair<T, Exception>> iterate22(final Collection<? extends Future<? extends T>> cfs) {
        return iterate22(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private static <T> ObjIterator<Pair<T, Exception>> iterate22(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll,
            final TimeUnit unit) {
        N.checkArgPositive(totalTimeoutForAll, "totalTimeoutForAll");
        N.checkArgNotNull(unit, "unit");

        final long now = System.currentTimeMillis();
        final long totalTimeoutForAllInMillis = totalTimeoutForAll == Long.MAX_VALUE ? Long.MAX_VALUE : unit.toMillis(totalTimeoutForAll);

        return new ObjIterator<Pair<T, Exception>>() {
            private final Set<Future<? extends T>> activeFutures = N.newSetFromMap(new IdentityHashMap<Future<? extends T>, Boolean>());
            {
                activeFutures.addAll(cfs);
            }

            @Override
            public boolean hasNext() {
                return activeFutures.size() > 0;
            }

            @Override
            public Pair<T, Exception> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                while (true) {
                    for (Future<? extends T> cf : activeFutures) {
                        if (cf.isDone()) {
                            try {
                                return Pair.<T, Exception> of(cf.get(), null);
                            } catch (Exception e) {
                                return Pair.of(null, e);
                            } finally {
                                activeFutures.remove(cf);
                            }
                        }
                    }

                    if (System.currentTimeMillis() - now >= totalTimeoutForAllInMillis) {
                        return Pair.<T, Exception> of(null, new TimeoutException());
                    }

                    N.sleepUninterruptibly(1);
                }
            }
        };
    }

    private static <R> R handle(final Pair<R, Exception> result) throws InterruptedException, ExecutionException {
        if (result.right != null) {
            if (result.right instanceof InterruptedException) {
                throw ((InterruptedException) result.right);
            } else if (result.right instanceof ExecutionException) {
                throw ((ExecutionException) result.right);
            } else {
                throw N.toRuntimeException(result.right);
            }
        }

        return result.left;
    }
}
