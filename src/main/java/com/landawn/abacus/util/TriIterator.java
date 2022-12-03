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
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.function.IndexedConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @param <A>
 * @param <B>
 * @param <C>
 * @since 1.2.10
 */
public abstract class TriIterator<A, B, C> extends ImmutableIterator<Triple<A, B, C>> {

    @SuppressWarnings("rawtypes")
    private static final TriIterator EMPTY = new TriIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void forEachRemaining(Throwables.TriConsumer action) throws Exception {
            N.checkArgNotNull(action);
        }

        @Override
        public ObjIterator map(TriFunction mapper) {
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
     * @param <A>
     * @param <B>
     * @param <C>
     * @param hasNext
     * @param output
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final BooleanSupplier hasNext, final Consumer<Triple<A, B, C>> output) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final Triple<A, B, C> tmp = new Triple<>();

            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                output.accept(tmp);

                return Triple.of(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                N.checkArgNotNull(action);

                while (hasNext.getAsBoolean()) {
                    output.accept(tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return hasNext.getAsBoolean();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
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
     * @param <A>
     * @param <B>
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param output
     * @return
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final int fromIndex, final int toIndex, final IndexedConsumer<Triple<A, B, C>> output) {
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
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                output.accept(cursor.getAndIncrement(), tmp);

                return Triple.of(tmp.left, tmp.middle, tmp.right);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                N.checkArgNotNull(action);

                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left, tmp.middle, tmp.right);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return cursor.value() < toIndex;
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
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
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return Triple.of(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                N.checkArgNotNull(action);

                while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
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
            public Triple<A, B, C> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return Triple.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                N.checkArgNotNull(action);

                while (iter1.hasNext() || iter2.hasNext() || iter3.hasNext()) {
                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
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
     * For each remaining.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E;

    /**
     * It's preferred to call <code>forEachRemaining(Try.TriConsumer)</code> to avoid the create the unnecessary <code>Triple</code> Objects.
     *
     * @param action
     * @deprecated
     */
    @Override
    @Deprecated
    public void forEachRemaining(java.util.function.Consumer<? super Triple<A, B, C>> action) {
        super.forEachRemaining(action);
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
     * @param <R>
     * @param mapper
     * @return
     */
    public <R> Stream<R> stream(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
        N.checkArgNotNull(mapper);

        return Stream.of(map(mapper));
    }

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

    public List<Triple<A, B, C>> toList() {
        return toCollection(Suppliers.ofList());
    }
}
