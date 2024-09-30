/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * The BiIterator class is an abstract class that extends ImmutableIterator.
 * It represents an iterator over a pair of values of type A and B.
 * This class provides a blueprint for classes that need to implement a bi-directional iterator.
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 *
 * @author Haiyang Li
 * @since 1.2.10
 */
@SuppressWarnings({ "java:S6548" })
public abstract class BiIterator<A, B> extends ImmutableIterator<Pair<A, B>> {

    @SuppressWarnings("rawtypes")
    private static final BiIterator EMPTY = new BiIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        protected void next(final Throwables.BiConsumer action) throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void forEachRemaining(final BiConsumer action) throws IllegalArgumentException {
            N.checkArgNotNull(action);
        }

        @Override
        public void foreachRemaining(final Throwables.BiConsumer action) throws IllegalArgumentException, Exception {
            N.checkArgNotNull(action);
        }

        @Override
        public ObjIterator map(final BiFunction mapper) throws IllegalArgumentException {
            N.checkArgNotNull(mapper);

            return ObjIterator.empty();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Throwables.BiConsumer DO_NOTHING = (a, b) -> {
        // do nothing;
    };

    /**
     *
     * @param <A>
     * @param <B>
     * @return
     */
    public static <A, B> BiIterator<A, B> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> BiIterator<K, V> of(final Map<K, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.entrySet().iterator());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param iter
     * @return
     */
    public static <K, V> BiIterator<K, V> of(final Iterator<Map.Entry<K, V>> iter) {
        if (iter == null) {
            return empty();
        }

        return new BiIterator<>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Pair<K, V> next() {
                return Pair.create(iter.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super K, ? super V, E> action) throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                final Map.Entry<K, V> entry = iter.next();

                action.accept(entry.getKey(), entry.getValue());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super K, ? super V> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super K, ? super V, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                Map.Entry<K, V> entry = null;

                while (iter.hasNext()) {
                    entry = iter.next();
                    action.accept(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super K, ? super V, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    private Map.Entry<K, V> entry = null;

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public R next() {
                        entry = iter.next();

                        return mapper.apply(entry.getKey(), entry.getValue());
                    }
                };
            }
        };
    }

    /**
     * Returns an infinite {@code BiIterator}.
     *
     * @param <A>
     * @param <B>
     * @param output transfer the next values.
     * @return
     */
    public static <A, B> BiIterator<A, B> generate(final Consumer<Pair<A, B>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     *
     *
     * @param <A>
     * @param <B>
     * @param hasNext
     * @param output
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B> BiIterator<A, B> generate(final BooleanSupplier hasNext, final Consumer<Pair<A, B>> output) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new BiIterator<>() {
            private final Pair<A, B> tmp = new Pair<>();

            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(tmp);

                return Pair.of(tmp.left, tmp.right);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(tmp);

                action.accept(tmp.left, tmp.right);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return hasNext.getAsBoolean();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        output.accept(tmp);

                        return mapper.apply(tmp.left, tmp.right);
                    }
                };
            }
        };
    }

    /**
     *
     *
     * @param <A>
     * @param <B>
     * @param fromIndex
     * @param toIndex
     * @param output
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static <A, B> BiIterator<A, B> generate(final int fromIndex, final int toIndex, final IntObjConsumer<Pair<A, B>> output)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
        N.checkArgNotNull(output);

        return new BiIterator<>() {
            private final MutableInt cursor = MutableInt.of(fromIndex);
            private final Pair<A, B> tmp = new Pair<>();

            @Override
            public boolean hasNext() {
                return cursor.value() < toIndex;
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Pair.of(tmp.left, tmp.right);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                action.accept(tmp.left, tmp.right);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return cursor.value() < toIndex;
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        output.accept(cursor.getAndIncrement(), tmp);

                        return mapper.apply(tmp.left, tmp.right);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b) {
        return zip(Array.asList(a), Array.asList(b));
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB) {
        return zip(Array.asList(a), Array.asList(b), valueForNoneA, valueForNoneB);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator());
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), valueForNoneA, valueForNoneB);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param iterA
     * @param iterB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB) {
        if (iterA == null || iterB == null) {
            return empty();
        }

        return new BiIterator<>() {
            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Pair.of(iterA.next(), iterB.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                action.accept(iterA.next(), iterB.next());
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext()) {
                    action.accept(iterA.next(), iterB.next());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext()) {
                    action.accept(iterA.next(), iterB.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterA.hasNext() && iterB.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.apply(iterA.next(), iterB.next());
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param iterA
     * @param iterB
     * @param valueForNoneA
     * @param valueForNoneB
     * @return
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB, final A valueForNoneA, final B valueForNoneB) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.<A> empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.<B> empty() : iterB;

        return new BiIterator<>() {
            @Override
            public boolean hasNext() {
                return iter1.hasNext() || iter2.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Pair.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iter1.hasNext() || iter2.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param iter
     * @param unzipFunc output parameter.
     * @return
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunc) {
        if (iter == null) {
            return BiIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunc);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param iter
     * @param unzipFunc output parameter.
     * @return
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunc) {
        if (iter == null) {
            return BiIterator.empty();
        }

        final BooleanSupplier hasNext = iter::hasNext;

        final Consumer<Pair<A, B>> output = out -> unzipFunc.accept(iter.next(), out);

        return BiIterator.generate(hasNext, output);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws NoSuchElementException
     * @throws E
     */
    protected abstract <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E;

    /**
     * It's preferred to call <code>forEachRemaining(Try.BiConsumer)</code> to avoid the create the unnecessary <code>Pair</code> Objects.
     *
     * @param action
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(final Consumer<? super Pair<A, B>> action) {
        super.forEachRemaining(action);
    }

    /**
     * For each remaining.
     *
     * @param action
     */
    public abstract void forEachRemaining(final BiConsumer<? super A, ? super B> action);

    /**
     * For each remaining.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E; // NOSONAR

    /**
     *
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public BiIterator<A, B> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!skipped) {
                    skip();
                }

                iter.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                if (!skipped) {
                    skip();
                }

                iter.forEachRemaining(action);
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                if (!skipped) {
                    skip();
                }

                iter.foreachRemaining(action);
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                if (!skipped) {
                    skip();
                }

                return iter.map(mapper);
            }

            private void skip() {
                long idx = 0;

                final Throwables.BiConsumer<A, B, RuntimeException> action = DO_NOTHING;

                while (idx++ < n && iter.hasNext()) {
                    iter.next(action);
                }

                skipped = true;
            }
        };
    }

    /**
     *
     *
     * @param count
     * @return
     * @throws IllegalArgumentException
     */
    public BiIterator<A, B> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return BiIterator.<A, B> empty();
        }

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                iter.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                final Throwables.BiConsumer<? super A, ? super B, RuntimeException> actionE = Fnn.from(action);

                while (hasNext()) {
                    cnt--;
                    iter.next(actionE);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                if (cnt > 0) {
                    return iter.<R> map(mapper).limit(cnt);
                } else {
                    return ObjIterator.<R> empty();
                }
            }
        };
    }

    /**
     *
     *
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public BiIterator<A, B> filter(final BiPredicate<? super A, ? super B> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final BiIterator<A, B> iter = this;

        return new BiIterator<>() {
            private final Pair<A, B> next = new Pair<>();
            private final Throwables.BiConsumer<A, B, RuntimeException> setNext = next::set;

            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        iter.next(setNext);

                        if (predicate.test(next.left, next.right)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public Pair<A, B> next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next.copy();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                action.accept(next.left, next.right);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                while (hasNext()) {
                    hasNext = false;

                    action.accept(next.left, next.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    hasNext = false;

                    action.accept(next.left, next.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNext) {
                            while (iter.hasNext()) {
                                iter.next(setNext);

                                if (predicate.test(next.left, next.right)) {
                                    hasNext = true;
                                    break;
                                }
                            }
                        }

                        return hasNext;
                    }

                    @Override
                    public R next() {
                        if (!hasNext && !hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNext = false;

                        return mapper.apply(next.left, next.right);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    public abstract <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper);

    /**
     *
     *
     * @return
     */
    public Optional<Pair<A, B>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.<Pair<A, B>> empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public Optional<Pair<A, B>> last() {
        if (hasNext()) {
            final Pair<A, B> next = new Pair<>();
            final Throwables.BiConsumer<A, B, RuntimeException> setNext = next::set;

            foreachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.<Pair<A, B>> empty();
        }
    }

    /**
     *
     * @return
     */
    public EntryStream<A, B> stream() {
        return EntryStream.of(this);
    }

    /**
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @throws IllegalArgumentException
     */
    public <R> Stream<R> stream(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper);

        return Stream.of(map(mapper));
    }

    /**
     *
     *
     * @return
     */
    public Pair<A, B>[] toArray() {
        return toArray(new Pair[0]);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     * @deprecated
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     *
     *
     * @return
     */
    public List<Pair<A, B>> toList() {
        return toCollection(Suppliers.<Pair<A, B>> ofList());
    }

    /**
     *
     *
     * @param supplier
     * @return
     */
    public Pair<List<A>, List<B>> toMultiList(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier) {
        final List<A> listA = supplier.get();
        final List<B> listB = supplier.get();

        this.foreachRemaining((a, b) -> {
            listA.add(a);
            listB.add(b);
        });

        return Pair.of(listA, listB);
    }

    /**
     *
     *
     * @param supplier
     * @return
     */
    public Pair<Set<A>, Set<B>> toMultiSet(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier) {
        final Set<A> listA = supplier.get();
        final Set<B> listB = supplier.get();

        this.foreachRemaining((a, b) -> {
            listA.add(a);
            listB.add(b);
        });

        return Pair.of(listA, listB);
    }
}
