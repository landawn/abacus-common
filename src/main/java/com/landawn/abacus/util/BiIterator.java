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
 * This class provides a blueprint for classes that need to implement a bidirectional iterator.
 *
 * @param <A> the first type of elements returned by this iterator
 * @param <B> the second type of elements returned by this iterator
 *
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 *
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
        public void foreachRemaining(final Throwables.BiConsumer action) throws IllegalArgumentException {
            N.checkArgNotNull(action);
        }

        @Override
        public ObjIterator map(final BiFunction mapper) throws IllegalArgumentException {
            N.checkArgNotNull(mapper, cs.mapper);

            return ObjIterator.empty();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Throwables.BiConsumer DO_NOTHING = (a, b) -> {
        // do nothing;
    };

    /**
     * Returns an empty BiIterator instance.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @return an empty BiIterator instance
     */
    public static <A, B> BiIterator<A, B> empty() {
        return EMPTY;
    }

    /**
     * Returns a BiIterator for the given map.
     * If the map is empty, returns an empty BiIterator.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the map to create the BiIterator from
     * @return a BiIterator over the entries of the map
     */
    public static <K, V> BiIterator<K, V> of(final Map<K, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.entrySet().iterator());
    }

    /**
     * Returns a BiIterator for the given iterator of map entries.
     * If the iterator is {@code null}, returns an empty BiIterator.
     *
     * @param <K> the type of keys in the map entries
     * @param <V> the type of values in the map entries
     * @param iter the iterator of map entries to create the BiIterator from
     * @return a BiIterator over the entries of the iterator
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
     * Generates an infinite {@code BiIterator} instance with the provided output Consumer.
     * The output Consumer is responsible for producing the next Pair<A, B> on each iteration.
     * This method is typically used for generating a BiIterator with custom logic.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param output A Consumer that accepts a Pair<A, B> and produces the next Pair<A, B> on each iteration.
     * @return A BiIterator<A, B> that uses the provided output Consumer to generate its elements.
     * @see #generate(BooleanSupplier, Consumer)
     */
    public static <A, B> BiIterator<A, B> generate(final Consumer<Pair<A, B>> output) {
        return generate(com.landawn.abacus.util.function.BooleanSupplier.TRUE, output);
    }

    /**
     * Generates a BiIterator instance with the provided hasNext BooleanSupplier and output Consumer.
     * The hasNext BooleanSupplier is used to determine if the iterator has more elements.
     * The output Consumer is responsible for producing the next Pair<A, B> on each iteration.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param hasNext A BooleanSupplier that returns {@code true} if the iterator has more elements.
     * @param output A Consumer that accepts a Pair<A, B> and produces the next Pair<A, B> on each iteration.
     * @return A BiIterator<A, B> that uses the provided hasNext BooleanSupplier and output Consumer to generate its elements.
     * @throws IllegalArgumentException If hasNext or output is {@code null}.
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
     * Generates a BiIterator instance with the provided fromIndex, toIndex, and output IntObjConsumer.
     * The fromIndex and toIndex define the size of the returned iterator.
     * The output IntObjConsumer is responsible for producing the next Pair<A, B> on each iteration.
     *
     * @param <A> the first type of elements returned by this iterator
     * @param <B> the second type of elements returned by this iterator
     * @param fromIndex The starting index of the iterator.
     * @param toIndex The ending index of the iterator.
     * @param output An IntObjConsumer that accepts an integer and a Pair<A, B> and produces the next Pair<A, B> on each iteration.
     * @return A BiIterator<A, B> that uses the provided fromIndex, toIndex, and output IntObjConsumer to generate its elements.
     * @throws IllegalArgumentException If fromIndex is greater than toIndex.
     * @throws IndexOutOfBoundsException If fromIndex or toIndex is out of range.
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
     * Zips two arrays into a BiIterator.
     * The resulting BiIterator will iterate over pairs of elements from the two arrays.
     * If the arrays have different lengths, the resulting BiIterator will have the length of the shorter array.
     * If either arrays is {@code null}, returns an empty BiIterator.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param a the first array
     * @param b the second array
     * @return a BiIterator that iterates over pairs of elements from the two arrays
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b) {
        return zip(Array.asList(a), Array.asList(b));
    }

    /**
     * Zips two arrays into a BiIterator with specified default values for missing elements.
     * The resulting BiIterator will iterate over pairs of elements from the two arrays.
     * If the arrays have different lengths, the resulting BiIterator will continue with the default values
     * for the shorter array until the longer array is exhausted.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param a the first array
     * @param b the second array
     * @param valueForNoneA the default value for missing elements in the first array
     * @param valueForNoneB the default value for missing elements in the second array
     * @return a BiIterator that iterates over pairs of elements from the two arrays
     */
    public static <A, B> BiIterator<A, B> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB) {
        return zip(Array.asList(a), Array.asList(b), valueForNoneA, valueForNoneB);
    }

    /**
     * Zips two iterables into a BiIterator.
     * The resulting BiIterator will iterate over pairs of elements from the two iterables.
     * If the iterables have different lengths, the resulting BiIterator will have the length of the shorter iterable.
     * If either iterable is {@code null}, returns an empty BiIterator.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param a the first iterable
     * @param b the second iterable
     * @return a BiIterator that iterates over pairs of elements from the two iterables
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator());
    }

    /**
     * Zips two iterables into a BiIterator with specified default values for missing elements.
     * The resulting BiIterator will iterate over pairs of elements from the two iterables.
     * If the iterables have different lengths, the resulting BiIterator will continue with the default values
     * for the shorter iterable until the longer iterable is exhausted.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param a the first iterable
     * @param b the second iterable
     * @param valueForNoneA the default value for missing elements in the first iterable
     * @param valueForNoneB the default value for missing elements in the second iterable
     * @return a BiIterator that iterates over pairs of elements from the two iterables
     */
    public static <A, B> BiIterator<A, B> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB) {
        return zip(a == null ? null : a.iterator(), b == null ? null : b.iterator(), valueForNoneA, valueForNoneB);
    }

    /**
     * Zips two iterators into a BiIterator.
     * The resulting BiIterator will iterate over pairs of elements from the two iterators.
     * If the iterators have different lengths, the resulting BiIterator will have the length of the shorter iterator.
     * If either iterator is {@code null}, returns an empty BiIterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @return a BiIterator that iterates over pairs of elements from the two iterators
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
     * Zips two iterators into a BiIterator with specified default values for missing elements.
     * The resulting BiIterator will iterate over pairs of elements from the two iterators.
     * If the iterators have different lengths, the resulting BiIterator will continue with the default values
     * for the shorter iterator until the longer iterator is exhausted.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param iterA the first iterator
     * @param iterB the second iterator
     * @param valueForNoneA the default value for missing elements in the first iterator
     * @param valueForNoneB the default value for missing elements in the second iterator
     * @return a BiIterator that iterates over pairs of elements from the two iterators
     */
    public static <A, B> BiIterator<A, B> zip(final Iterator<A> iterA, final Iterator<B> iterB, final A valueForNoneA, final B valueForNoneB) {
        final Iterator<A> iter1 = iterA == null ? ObjIterator.empty() : iterA;
        final Iterator<B> iter2 = iterB == null ? ObjIterator.empty() : iterB;

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
     * Unzips an iterable of elements into a BiIterator.
     * The resulting BiIterator will iterate over pairs of elements produced by the unzip function.
     * If the iterable is {@code null}, returns an empty BiIterator.
     *
     * @param <T> the type of elements in the iterable
     * @param <A> the first type of elements returned by the BiIterator
     * @param <B> the second type of elements returned by the BiIterator
     * @param iter the iterable to unzip
     * @param unzipFunc a BiConsumer that accepts an element of type T and a {@code Pair<A, B>} and populates the pair with the unzipped values
     * @return a BiIterator that iterates over pairs of elements produced by the unzip function
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzipFunc) {
        if (iter == null) {
            return BiIterator.empty();
        }

        return unzip(iter.iterator(), unzipFunc);
    }

    /**
     * Unzips an iterator of elements into a BiIterator.
     * The resulting BiIterator will iterate over pairs of elements produced by the unzip function.
     * If the iterator is {@code null}, returns an empty BiIterator.
     *
     * @param <T> the type of elements in the iterator
     * @param <A> the first type of elements returned by the BiIterator
     * @param <B> the second type of elements returned by the BiIterator
     * @param iter the iterator to unzip
     * @param unzipFunc a BiConsumer that accepts an element of type T and a {@code Pair<A, B>} and populates the pair with the unzipped values
     * @return a BiIterator that iterates over pairs of elements produced by the unzip function
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
     * Processes the next pair of elements in the iterator using the provided action.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a BiConsumer that processes the next pair of elements
     * @throws NoSuchElementException if there are no more elements in the iterator
     * @throws E if the action throws an exception
     */
    protected abstract <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E;

    /**
     * Performs the given action for each remaining element in the iterator until all elements have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     * @deprecated use {@code forEachRemaining(BiConsumer)} to avoid creating the unnecessary {@code Pair} Objects.
     * @see #forEachRemaining(BiConsumer)
     */
    @Override
    @Deprecated
    public void forEachRemaining(final Consumer<? super Pair<A, B>> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element in the iterator until all elements have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each element
     */
    public abstract void forEachRemaining(final BiConsumer<? super A, ? super B> action);

    /**
     * Performs the given action for each remaining element in the iterator until all elements
     * have been processed or the action throws an exception.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a BiConsumer that processes the next pair of elements
     * @throws E if the action throws an exception
     */
    public abstract <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E; // NOSONAR

    /**
     * Returns a new BiIterator with <i>n</i> elements skipped from the beginning of this BiIterator.
     *
     * @param n the number of elements to skip
     * @return A new BiIterator that skips the first <i>n</i> elements.
     * @throws IllegalArgumentException If <i>n</i> is negative.
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
                N.checkArgNotNull(mapper, cs.mapper);

                if (!skipped) {
                    skip();
                }

                return iter.map(mapper);
            }

            private void skip() {
                long idx = 0;

                @SuppressWarnings("UnnecessaryLocalVariable")
                final Throwables.BiConsumer<A, B, RuntimeException> action = DO_NOTHING;

                while (idx++ < n && iter.hasNext()) {
                    iter.next(action);
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new BiIterator with a limited number of elements.
     * The resulting BiIterator will contain at most the specified number of elements.
     *
     * @param count the maximum number of elements to include in the resulting BiIterator
     * @return a new BiIterator that contains at most the specified number of elements
     * @throws IllegalArgumentException If <i>count</i> is negative.
     */
    public BiIterator<A, B> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return BiIterator.empty();
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
                N.checkArgNotNull(mapper, cs.mapper);

                if (cnt > 0) {
                    return iter.<R> map(mapper).limit(cnt);
                } else {
                    return ObjIterator.empty();
                }
            }
        };
    }

    /**
     * Returns a new BiIterator that includes only the elements that satisfy the provided predicate.
     *
     * @param predicate the predicate to apply to each pair of elements
     * @return a new BiIterator containing only the elements that match the predicate
     */
    public BiIterator<A, B> filter(final BiPredicate<? super A, ? super B> predicate) {
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
                N.checkArgNotNull(mapper, cs.mapper);

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
     * Transforms the elements of this BiIterator using the given mapper function.
     *
     * @param <R> the type of elements in the resulting ObjIterator
     * @param mapper the function to apply to each pair of elements
     * @return an ObjIterator containing the elements produced by the mapper function
     */
    public abstract <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper);

    /**
     * Returns an Optional containing the first pair of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * @return an Optional containing the first pair of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Pair<A, B>> first() {
        if (hasNext()) {
            return Optional.of(next());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an Optional containing the last pair of elements in the iterator.
     * If the iterator is empty, returns an empty Optional.
     *
     * @return an Optional containing the last pair of elements, or an empty Optional if the iterator is empty
     */
    public Optional<Pair<A, B>> last() {
        if (hasNext()) {
            final Pair<A, B> next = new Pair<>();
            final BiConsumer<A, B> setNext = next::set;

            forEachRemaining(setNext);

            return Optional.of(next);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns an EntryStream of the elements in this BiIterator.
     *
     * @return A Stream containing the remaining elements in the BiIterator.
     */
    public EntryStream<A, B> stream() {
        return EntryStream.of(this);
    }

    /**
     * Returns a Stream of elements produced by applying the given mapper function to each pair of elements in this BiIterator.
     *
     * @param <R> the type of elements in the resulting Stream
     * @param mapper the function to apply to each pair of elements
     * @return a Stream containing the elements produced by the mapper function
     */
    public <R> Stream<R> stream(final BiFunction<? super A, ? super B, ? extends R> mapper) {
        N.checkArgNotNull(mapper, cs.mapper);

        return Stream.of(map(mapper));
    }

    /**
     * Converts the elements in this BiIterator to an array of Pair objects.
     *
     * @return An array containing the remaining pairs of elements in this BiIterator.
     */
    public Pair<A, B>[] toArray() {
        return toArray(new Pair[0]);
    }

    /**
     * Converts the elements in this BiIterator to an array of the specified type.
     *
     * @param <T> the type of the array elements. It should be super type of Pair.
     * @param a the array into which the elements of this BiIterator are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose.
     * @return an array containing the elements of this BiIterator
     * @deprecated This method is deprecated. Use {@link #toArray()} or {@link #toList()} instead.
     */
    @Deprecated
    public <T> T[] toArray(final T[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the elements in this BiIterator to a List of Pair objects.
     *
     * @return a List containing all pairs of elements in this BiIterator
     */
    public List<Pair<A, B>> toList() {
        return toCollection(Suppliers.ofList());
    }

    /**
     * Converts the elements in this BiIterator to a Pair of Lists.
     * The first list contains all the first elements of the pairs, and the second list contains all the second elements of the pairs.
     *
     * @param supplier a Supplier that provides new instances of List
     * @return a Pair containing two Lists: one with the first elements and one with the second elements
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
     * Converts the elements in this BiIterator to a Pair of Sets.
     * The first set contains all the first elements of the pairs, and the second set contains all the second elements of the pairs.
     *
     * @param supplier a Supplier that provides new instances of Set
     * @return a Pair containing two Sets: one with the first elements and one with the second elements
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
