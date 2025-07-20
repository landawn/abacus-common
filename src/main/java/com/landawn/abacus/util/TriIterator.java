/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * The TriIterator class is an abstract class that extends ImmutableIterator.
 * It represents an iterator over a triple of values of type A, B, and C.
 * This class provides a blueprint for classes that need to implement a tri-directional iterator.
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 * @param <C> the third type of elements returned by this iterator
 *
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 *
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
        protected void next(final Throwables.TriConsumer action) throws NoSuchElementException {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public void forEachRemaining(final TriConsumer action) throws IllegalArgumentException {
        }

        @Override
        public void foreachRemaining(final Throwables.TriConsumer action) throws IllegalArgumentException {
        }

        @Override
        public ObjIterator map(final TriFunction mapper) throws IllegalArgumentException {
            return ObjIterator.empty();
        }
    };

    /**
     * Returns an empty TriIterator.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @return an empty TriIterator
     */
    public static <A, B, C> TriIterator<A, B, C> empty() {
        return EMPTY;
    }

    /**
     * Generates an infinite {@code TriIterator} instance with the provided output Consumer.
     * The output Consumer is responsible for producing the next Triple<A, B, C> on each iteration.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param output A Consumer that accepts a Triple<A, B, C> and produces the next Triple<A, B, C> on each iteration.
     * @return A TriIterator<A, B, C> that uses the provided output Consumer to generate its elements.
     * @see  #generate(BooleanSupplier, Consumer)
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final Consumer<Triple<A, B, C>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     * Generates a TriIterator instance with the provided hasNext BooleanSupplier and output Consumer.
     * The hasNext BooleanSupplier is used to determine if the iterator has more elements.
     * The output Consumer is responsible for producing the next Triple<A, B, C> on each iteration.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param hasNext A BooleanSupplier that returns {@code true} if the iterator has more elements.
     * @param output A Consumer that accepts a Triple<A, B, C> and produces the next Triple<A, B, C> on each iteration.
     * @return A TriIterator<A, B, C> that uses the provided hasNext BooleanSupplier and output Consumer to generate its elements.
     * @throws IllegalArgumentException If hasNext or output is {@code null}.
     */
    public static <A, B, C> TriIterator<A, B, C> generate(final BooleanSupplier hasNext, final Consumer<Triple<A, B, C>> output)
            throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(output);

        return new TriIterator<>() {
            private final Triple<A, B, C> tmp = new Triple<>();
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = hasNext.getAsBoolean();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                return Triple.of(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                output.accept(tmp);

                action.accept(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                while (hasNextFlag || hasNext()) {
                    hasNextFlag = false;

                    output.accept(tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = hasNext.getAsBoolean();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        output.accept(tmp);

                        return mapper.apply(tmp.left(), tmp.middle(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Generates a TriIterator instance with the provided fromIndex, toIndex, and output IntObjConsumer.
     * The fromIndex and toIndex define the size of the returned iterator.
     * The output IntObjConsumer is responsible for producing the next Triple<A, B, C> on each iteration.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param <C> the third type of elements returned by this iterator
     * @param fromIndex The starting index of the iterator.
     * @param toIndex The ending index of the iterator.
     * @param output An IntObjConsumer that accepts an integer and a Triple<A, B, C> and produces the next Triple<A, B, C> on each iteration.
     * @return A TriIterator<A, B, C> that uses the provided fromIndex, toIndex, and output IntObjConsumer to generate its elements.
     * @throws IllegalArgumentException If fromIndex is greater than toIndex.
     * @throws IndexOutOfBoundsException If fromIndex or toIndex is out of range.
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

                return Triple.of(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                output.accept(cursor.getAndIncrement(), tmp);

                action.accept(tmp.left(), tmp.middle(), tmp.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                while (cursor.value() < toIndex) {
                    output.accept(cursor.getAndIncrement(), tmp);

                    action.accept(tmp.left(), tmp.middle(), tmp.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {

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

                        return mapper.apply(tmp.left(), tmp.middle(), tmp.right());
                    }
                };
            }
        };
    }

    /**
     * Zips three arrays into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three arrays.
     * If the arrays have different lengths, the resulting TriIterator will have the length of the shortest array.
     * If any of arrays is {@code null}, returns an empty TriIterator.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param a the first array
     * @param b the second array
     * @param c the third array
     * @return a TriIterator that iterates over the elements of the three arrays in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c));
    }

    /**
     * Zips three arrays into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three arrays.
     * If the arrays have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter array until the longest array is exhausted.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param a the first array
     * @param b the second array
     * @param c the third array
     * @param valueForNoneA the default value for missing elements in the first array
     * @param valueForNoneB the default value for missing elements in the second array
     * @param valueForNoneC the default value for missing elements in the third array
     * @return a TriIterator that iterates over the elements of the three arrays in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC) {
        return zip(Array.asList(a), Array.asList(b), Array.asList(c), valueForNoneA, valueForNoneB, valueForNoneC);
    }

    /**
     * Zips three iterables into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three iterables.
     * If the iterables have different lengths, the resulting TriIterator will have the length of the shortest iterable.
     * If any of iterable is {@code null}, returns an empty TriIterator.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param a the first iterable
     * @param b the second iterable
     * @param c the third iterable
     * @return a TriIterator that iterates over the elements of the three iterables in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator());
    }

    /**
     * Zips three iterables into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three iterables.
     * If the iterables have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter iterable until the longest iterable is exhausted.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param a the first iterable
     * @param b the second iterable
     * @param c the third iterable
     * @param valueForNoneA the default value for missing elements in the first iterable
     * @param valueForNoneB the default value for missing elements in the second iterable
     * @param valueForNoneC the default value for missing elements in the third iterable
     * @return a TriIterator that iterates over the elements of the three iterables in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), c == null ? null : c.iterator(), valueForNoneA, valueForNoneB,
                valueForNoneC);
    }

    /**
     * Zips three iterators into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements from the three iterators.
     * If the iterators have different lengths, the resulting TriIterator will have the length of the shortest iterator.
     * If any of iterator is {@code null}, returns an empty TriIterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @param iterC the third iterator
     * @return a TriIterator that iterates over the elements of the three iterators in parallel
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC) {
        if (iterA == null || iterB == null || iterC == null) {
            return empty();
        }

        return new TriIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Triple.of(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                // N.checkArgNotNull(action);
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iterA.next(), iterB.next(), iterC.next());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {

                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext() && iterC.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                while (hasNextFlag || (iterA.hasNext() && iterB.hasNext() && iterC.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iterA.next(), iterB.next(), iterC.next());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iterA.next(), iterB.next(), iterC.next());
                    }
                };
            }
        };
    }

    /**
     * Zips three iterators into a TriIterator with specified default values for missing elements.
     * The resulting TriIterator will iterate over triples of elements from the three iterators.
     * If the iterators have different lengths, the resulting TriIterator will continue with the default values
     * for the shorter iterator until the longest iterator is exhausted.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @param iterC the third iterator
     * @param valueForNoneA the default value for missing elements in the first iterator
     * @param valueForNoneB the default value for missing elements in the second iterator
     * @param valueForNoneC the default value for missing elements in the third iterator
     * @return a TriIterator that iterates over the elements of the three iterators in parallel, using default values for missing elements
     */
    public static <A, B, C> TriIterator<A, B, C> zip(final Iterator<A> iterA, final Iterator<B> iterB, final Iterator<C> iterC, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.empty() : iterB;
        final Iterator<C> iter3 = iterC == null ? ObjIterator.empty() : iterC;

        return new TriIterator<>() {
            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    hasNextFlag = iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() throws IllegalArgumentException {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                return Triple.of(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!(hasNextFlag || hasNext())) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false; // Reset for the next call

                action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                        iter3.hasNext() ? iter3.next() : valueForNoneC);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext() || iter3.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws IllegalArgumentException, E {
                while (hasNextFlag || (iter1.hasNext() || iter2.hasNext() || iter3.hasNext())) {
                    hasNextFlag = false; // Reset for the next call

                    action.accept(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                            iter3.hasNext() ? iter3.next() : valueForNoneC);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            hasNextFlag = iter1.hasNext() || iter2.hasNext() || iter3.hasNext();
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!(hasNextFlag || hasNext())) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false; // Reset for the next call

                        return mapper.apply(iter1.hasNext() ? iter1.next() : valueForNoneA, iter2.hasNext() ? iter2.next() : valueForNoneB,
                                iter3.hasNext() ? iter3.next() : valueForNoneC);
                    }
                };
            }
        };
    }

    /**
     * Unzips an iterable of elements into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements produced by the unzip function.
     * If the iterable is {@code null}, an empty TriIterator is returned.
     *
     * @param <T> the type of elements in the input iterable
     * @param <A> the type of elements in the first component of the triple
     * @param <B> the type of elements in the second component of the triple
     * @param <C> the type of elements in the third component of the triple
     * @param iter the input iterable
     * @param unzipFunc a BiConsumer that accepts an element of type T and a {@code Triple<A, B, C>} and populates the triple with the unzipped values
     * @return a TriIterator that iterates over the unzipped elements
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunc) {
        if (iter == null) {
            return TriIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunc);
    }

    /**
     * Unzips an iterator of elements into a TriIterator.
     * The resulting TriIterator will iterate over triples of elements produced by the unzip function.
     * If the iterator is {@code null}, an empty TriIterator is returned.
     *
     * @param <T> the type of elements in the input iterator
     * @param <A> the type of elements in the first component of the triple
     * @param <B> the type of elements in the second component of the triple
     * @param <C> the type of elements in the third component of the triple
     * @param iter the input iterator
     * @param unzipFunc a BiConsumer that accepts an element of type T and a Triple<A, B, C> and populates the triple with the unzipped values
     * @return a TriIterator that iterates over the unzipped elements
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzipFunc) {
        if (iter == null) {
            return TriIterator.empty();
        }

        final BooleanSupplier booleanSupplier = iter::hasNext;

        final Consumer<Triple<A, B, C>> output = out -> unzipFunc.accept(iter.next(), out);

        return TriIterator.generate(booleanSupplier, output);
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
     * Performs the given action for each remaining element in the iterator until all elements have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     * @deprecated use {@code forEachRemaining(TriConsumer)} to avoid creating the unnecessary {@code Triple} Objects.
     * @see #forEachRemaining(TriConsumer)
     */
    @Deprecated
    @Override
    public void forEachRemaining(final Consumer<? super Triple<A, B, C>> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     */
    public abstract void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action);

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element
     * @throws E if the action throws an exception
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E; // NOSONAR

    @SuppressWarnings("rawtypes")
    private static final Throwables.TriConsumer DO_NOTHING = (a, b, c) -> {
        // do nothing;
    };

    /**
     * Returns a new TriIterator with <i>n</i> elements skipped from the beginning of this TriIterator.
     *
     * @param n the number of elements to skip
     * @return A new TriIterator that skips the first <i>n</i> elements.
     * @throws IllegalArgumentException If <i>n</i> is negative.
     */
    public TriIterator<A, B, C> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

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
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
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

                while (idx++ < n && iter.hasNext()) {
                    iter.next(DO_NOTHING);
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new TriIterator with a limited number of elements.
     * The resulting TriIterator will contain at most the specified number of elements.
     *
     * @param count the maximum number of elements to include in the resulting TriIterator
     * @return a new TriIterator that contains at most the specified number of elements
     * @throws IllegalArgumentException If <i>count</i> is negative.
     */
    public TriIterator<A, B, C> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return TriIterator.empty();
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
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
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
                    return ObjIterator.empty();
                }
            }
        };
    }

    /**
     * Returns a new TriIterator that includes only the elements that satisfy the provided predicate.
     *
     * @param predicate the predicate to apply to each pair of elements
     * @return a new TriIterator containing only the elements that match the predicate
     */
    public TriIterator<A, B, C> filter(final TriPredicate<? super A, ? super B, ? super C> predicate) {
        N.checkArgNotNull(predicate, cs.Predicate);

        final TriIterator<A, B, C> iter = this;

        return new TriIterator<>() {
            private final Triple<A, B, C> next = new Triple<>();
            private final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            private boolean hasNextFlag = false;

            @Override
            public boolean hasNext() {
                if (!hasNextFlag) {
                    while (iter.hasNext()) {
                        iter.next(setNext);

                        if (predicate.test(next.left(), next.middle(), next.right())) {
                            hasNextFlag = true;
                            break;
                        }
                    }
                }

                return hasNextFlag;
            }

            @Override
            public Triple<A, B, C> next() {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                return next.copy();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if (!hasNextFlag && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextFlag = false;

                action.accept(next.left(), next.middle(), next.right());
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.middle(), next.right());
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    hasNextFlag = false;

                    action.accept(next.left(), next.middle(), next.right());
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
                return new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        if (!hasNextFlag) {
                            while (iter.hasNext()) {
                                iter.next(setNext);

                                if (predicate.test(next.left(), next.middle(), next.right())) {
                                    hasNextFlag = true;
                                    break;
                                }
                            }
                        }

                        return hasNextFlag;
                    }

                    @Override
                    public R next() {
                        if (!hasNextFlag && !hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNextFlag = false;

                        return mapper.apply(next.left(), next.middle(), next.right());
                    }
                };
            }
        };
    }

    /**
     * Transforms the elements of this TriIterator using the given mapper function.
     *
     * @param <R> the type of elements in the resulting ObjIterator
     * @param mapper the function to apply to each triple of elements
     * @return an ObjIterator containing the elements produced by the mapper function
     */
    public abstract <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper);

    /**
     * Returns an Optional containing the first triple of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * @return an Optional containing the first triple of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Triple<A, B, C>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an Optional containing the last triple of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * @return an Optional containing the last triple of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Triple<A, B, C>> last() {
        if (hasNext()) {
            final Triple<A, B, C> next = new Triple<>();
            final Throwables.TriConsumer<A, B, C, RuntimeException> setNext = next::set;

            foreachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns a Stream of elements produced by applying the given mapper function to each triple of elements in this TriIterator.
     *
     * @param <R> the type of elements in the resulting Stream
     * @param mapper the function to apply to each triple of elements
     * @return a Stream containing the elements produced by the mapper function
     */
    public <R> Stream<R> stream(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
        return Stream.of(map(mapper));
    }

    /**
     * Converts the elements in this TriIterator to an array of Triple objects.
     *
     * @return An array containing the remaining triples of elements in this TriIterator.
     */
    public Triple<A, B, C>[] toArray() {
        return toArray(new Triple[0]);
    }

    /**
     * Converts the elements in this TriIterator to an array of the specified type.
     *
     * @param <T> the type of the array elements. It should be a super type of Triple.
     * @param a the array into which the elements of this TriIterator are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of this TriIterator
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the elements in this TriIterator to a List of Triple objects.
     *
     * @return a List containing all triples of elements in this TriIterator
     */
    public List<Triple<A, B, C>> toList() {
        return toCollection(Suppliers.ofList());
    }

    /**
     * Converts the elements in this TriIterator to three separate lists of type A, B, and C.
     * The resulting Triple contains three lists, each containing the elements of the corresponding type.
     *
     * @param supplier a Supplier that provides new instances of List for storing the elements
     * @return a Triple containing three lists of elements of type A, B, and C
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
     * Converts the elements in this TriIterator to three separate sets of type A, B, and C.
     * The resulting Triple contains three sets, each containing the elements of the corresponding type.
     *
     * @param supplier a Supplier that provides new instances of Set for storing the elements
     * @return a Triple containing three sets of elements of type A, B, and C
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
