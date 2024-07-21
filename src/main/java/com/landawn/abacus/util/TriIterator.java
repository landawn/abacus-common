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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @param <A>
 * @param <B>
 * @param <C>
 * @since 1.2.10
 */
@SuppressWarnings({ "java:S6548" })
public abstract class TriIterator<A, B, C> extends ImmutableIterator<Triple<A, B, C>> {

    @SuppressWarnings("rawtypes")
    private static final TriIterator EMPTY = new TriIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        protected void next(Throwables.TriConsumer action) throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void forEachRemaining(TriConsumer action) throws IllegalArgumentException {
            N.checkArgNotNull(action);
        }

        @Override
        public void foreachRemaining(Throwables.TriConsumer action) throws IllegalArgumentException, Exception {
            N.checkArgNotNull(action);
        }

        @Override
        public ObjIterator map(TriFunction mapper) throws IllegalArgumentException {
            N.checkArgNotNull(mapper);

            return ObjIterator.empty();
        }
    };

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> empty() {
        return EMPTY;
    }

    /**
     * Returns an infinite {@code BiIterator}.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param output transfer the next values.
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final Consumer<Triple<A, B, C>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     *
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param hasNext
     * @param output
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final BooleanSupplier hasNext, final Consumer<Triple<A, B, C>> output)
            throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final Triple<A, B, C> tmp = new Triple<>();

            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(tmp);

                return Triple.of(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(tmp);

                action.accept(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
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

                        return mapper.apply(tmp.left, tmp.middle, tmp.right);
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
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param output
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final int fromIndex, final int toIndex, final IntObjConsumer<Triple<A, B, C>> output)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final MutableInt cursor = MutableInt.of(fromIndex);
            private final Triple<A, B, C> tmp = new Triple<>();

            @Override
            public boolean hasNext() {
                return cursor.value() < toIndex;
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Triple.of(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                action.accept(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
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

                        return mapper.apply(tmp.left, tmp.middle, tmp.right);
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c));
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c), valueForNoneA, valueForNoneB, valueForNoneC);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator());
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator(), valueForNoneA, valueForNoneB,
                valueForNoneC);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param iterA
     * @param iterB
     * @param iterC
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC) {
        if (iterA == null || iterB == null || iterC == null) {
            return empty();
        }

        return new TriIterator<>() {
            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Triple.of(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                action.accept(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.apply(iterA.next(), iterB.next(), iterC.next());
                    }
                };
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param iterA
     * @param iterB
     * @param iterC
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.<A> empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.<B> empty() : iterB;
        final Iterator<C> iter3 = iterC == null ? ObjIterator.<C> empty() : iterC;

        return new TriIterator<>() {
            @Override
            public boolean hasNext() {
                return iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Triple.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);

                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext() || iter3.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext() || iter3.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                                iter3.hasNext() ? iter3.next() : valueForNoneC);
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
     * @param <C>
     * @param iter
     * @param unzipFunc output parameter.
     * @return
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunc) {
        if (iter == null) {
            return TriIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunc);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param iter
     * @param unzipFunc output parameter.
     * @return
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunc) {
        if (iter == null) {
            return TriIterator.empty();
        }

        final BooleanSupplier hasNext = iter::hasNext;

        final Consumer<Triple<A, B, C>> output = out -> unzipFunc.accept(iter.next(), out);

        return TriIterator.generate(hasNext, output);
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws NoSuchElementException
     * @throws E
     */
    protected abstract <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
            throws NoSuchElementException, E;

    /**
     * It's preferred to call <code>forEachRemaining(Try.TriConsumer)</code> to avoid the create the unnecessary <code>Triple</code> Objects.
     *
     * @param action
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(Consumer<? super Triple<A, B, C>> action) {
        super.forEachRemaining(action);
    }

    /**
     * For each remaining.
     *
     * @param action
     */
    public abstract void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action);

    /**
     * For each remaining.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E; // NOSONAR

    @SuppressWarnings("rawtypes")
    private static final Throwables.TriConsumer DO_NOTHING = (a, b, c) -> {
        // do nothing;
    };

    /**
     *
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public TriIterator<A, B, C> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, "n");

        if (n <= 0) {
            return this;
        }

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!skipped) {
                    skip();
                }

                iter.next(action);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) {
                if (!skipped) {
                    skip();
                }

                iter.forEachRemaining(action);
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                if (!skipped) {
                    skip();
                }

                iter.foreachRemaining(action);
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                if (!skipped) {
                    skip();
                }

                return iter.map(mapper);
            }

            private void skip() {
                long idx = 0;

                final Throwables.TriConsumer<A, B, C, RuntimeException> action = DO_NOTHING;

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
    public TriIterator<A, B, C> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, "count");

        if (count == 0) {
            return TriIterator.<A, B, C> empty();
        }

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                iter.next(action);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    cnt--;
                    iter.next(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
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
    public TriIterator<A, B, C> filter(final TriPredicate<? super A, ? super B, ? super C> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, "predicate");

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private final Triple<A, B, C> next = new Triple<>();
            private final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        iter.next(setNext);

                        if (predicate.test(next.left, next.middle, next.right)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next.copy();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                action.accept(next.left, next.middle, next.right);
            }

            @Override
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    hasNext = false;

                    action.accept(next.left, next.middle, next.right);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    hasNext = false;

                    action.accept(next.left, next.middle, next.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNext) {
                            while (iter.hasNext()) {
                                iter.next(setNext);

                                if (predicate.test(next.left, next.middle, next.right)) {
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

                        return mapper.apply(next.left, next.middle, next.right);
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
    public abstract <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper);

    /**
     *
     *
     * @return
     */
    public Optional<Triple<A, B, C>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.<Triple<A, B, C>> empty();
        }
    }

    /**
     *
     *
     * @return
     */
    public Optional<Triple<A, B, C>> last() {
        if (hasNext()) {
            final Triple<A, B, C> next = new Triple<>();
            final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            foreachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.<Triple<A, B, C>> empty();
        }
    }

    /**
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @throws IllegalArgumentException
     */
    public <R> Stream<R> stream(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper);

        return Stream.of(map(mapper));
    }

    /**
     *
     *
     * @return
     */
    public Triple<A, B, C>[] toArray() {
        return toArray(new Triple[0]);
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
    public List<Triple<A, B, C>> toList() {
        return toCollection(Suppliers.ofList());
    }

    /**
     *
     *
     * @param supplier
     * @return
     */
    public Triple<List<A>, List<B>, List<C>> toMultiList(@SuppressWarnings("rawtypes") final Supplier<? extends List> supplier) {
        final List<A> listA = supplier.get();
        final List<B> listB = supplier.get();
        final List<C> listC = supplier.get();

        this.foreachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });

        return Triple.of(listA, listB, listC);
    }

    /**
     *
     *
     * @param supplier
     * @return
     */
    public Triple<Set<A>, Set<B>, Set<C>> toMultiSet(@SuppressWarnings("rawtypes") final Supplier<? extends Set> supplier) {
        final Set<A> listA = supplier.get();
        final Set<B> listB = supplier.get();
        final Set<C> listC = supplier.get();

        this.foreachRemaining((a, b, c) -> {
            listA.add(a);
            listB.add(b);
            listC.add(c);
        });

        return Triple.of(listA, listB, listC);
    }
}
